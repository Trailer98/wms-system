package com.example.wms.admin.aspect;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.common.common.ForbiddenException;
import com.example.wms.common.common.UnauthorizedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs before the target method executes (permission failures throw here, so the method body never
 * runs). Ordered ahead of SysOperationAspect so a rejected call still gets a chance to be logged as
 * a failed attempt by that aspect once it wraps proceed() in a try/catch.
 */
@Aspect
@Component
@Order(1)
public class PermissionAspect {

    @Around("@annotation(requiresPermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser == null) {
            throw new UnauthorizedException("login required");
        }
        if (!currentUser.hasPermission(requiresPermission.value())) {
            throw new ForbiddenException("missing permission: " + requiresPermission.value());
        }
        return joinPoint.proceed();
    }
}
