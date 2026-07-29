package com.example.wms.admin.security;

import com.example.wms.common.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LegacyAdminAccessInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final boolean legacyAdminEnabled;

    public LegacyAdminAccessInterceptor(
            ObjectMapper objectMapper,
            @Value("${wms.auth.legacy-admin-enabled:false}") boolean legacyAdminEnabled) {
        this.objectMapper = objectMapper;
        this.legacyAdminEnabled = legacyAdminEnabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (legacyAdminEnabled) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(
                403,
                "WMS legacy user/role/permission management is deprecated. Please manage RBAC in auth-service.")));
        return false;
    }
}
