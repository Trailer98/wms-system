package com.example.wms.admin.aspect;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.client.AuthContextResponse;
import com.example.wms.admin.client.AuthServiceClient;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.common.common.ForbiddenException;
import com.example.wms.common.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Runs before the target method executes (permission failures throw here, so the method body never
 * runs). Ordered ahead of SysOperationAspect so a rejected call still gets a chance to be logged as
 * a failed attempt by that aspect once it wraps proceed() in a try/catch.
 */
@Aspect
@Component
@Order(1)
public class PermissionAspect {

    private final AuthServiceClient authServiceClient;
    private final HttpServletRequest request;

    public PermissionAspect(AuthServiceClient authServiceClient, HttpServletRequest request) {
        this.authServiceClient = authServiceClient;
        this.request = request;
    }

    @Around("@annotation(requiresPermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser == null) {
            throw new UnauthorizedException("login required");
        }
        AuthContextResponse authContext = authServiceClient.getCurrentUserContext(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (authContext.userId() == null || !authContext.userId().equals(currentUser.userId())) {
            throw new UnauthorizedException("gateway user does not match authorization context");
        }
        if (CollectionUtils.isEmpty(authContext.permissions())) {
            throw new ForbiddenException("empty permission context");
        }

        Set<String> roleCodes = authContext.roles() == null ? Set.of() : new LinkedHashSet<>(authContext.roles());
        Set<String> permissionCodes = new LinkedHashSet<>(authContext.permissions());
        CurrentUser authorizedUser = currentUser.withAuthorizationContext(roleCodes, permissionCodes);
        CurrentUserContext.set(authorizedUser);

        if (!authorizedUser.hasPermission(requiresPermission.value())) {
            throw new ForbiddenException("missing permission: " + requiresPermission.value());
        }
        return joinPoint.proceed();
    }
}
