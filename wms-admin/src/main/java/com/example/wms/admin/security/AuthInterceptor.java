package com.example.wms.admin.security;

import com.example.wms.common.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * @deprecated WMS authentication now comes from Gateway identity headers and auth-service permission
 * context. Kept temporarily as a rollback path for the legacy local JWT flow.
 *
 * Parses {@code Authorization: Bearer <token>} on every request that isn't explicitly excluded
 * (see WebMvcConfig) and populates CurrentUserContext for the duration of the request. Missing or
 * invalid tokens short-circuit with a 401 written directly here (before the request ever reaches a
 * controller / the PermissionAspect).
 */
@Deprecated(forRemoval = false)
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String header = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "missing or malformed Authorization header");
            return false;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            CurrentUser currentUser = jwtTokenService.parseToken(token);
            CurrentUserContext.set(currentUser);
            return true;
        } catch (RuntimeException ex) {
            writeUnauthorized(response, ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(401, message)));
    }
}
