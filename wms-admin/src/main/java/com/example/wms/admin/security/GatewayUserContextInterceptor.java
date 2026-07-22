package com.example.wms.admin.security;

import com.example.wms.common.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class GatewayUserContextInterceptor implements HandlerInterceptor {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_TOKEN_ID = "X-Token-Id";
    public static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    private final ObjectMapper objectMapper;
    private final String gatewayInternalToken;

    public GatewayUserContextInterceptor(
            ObjectMapper objectMapper,
            @Value("${gateway.internal-token:}") String gatewayInternalToken) {
        this.objectMapper = objectMapper;
        this.gatewayInternalToken = gatewayInternalToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!isTrustedGatewayRequest(request)) {
            writeUnauthorized(response, "missing or invalid gateway token");
            return false;
        }

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        if (!StringUtils.hasText(userIdHeader)) {
            writeUnauthorized(response, "missing X-User-Id header");
            return false;
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            writeUnauthorized(response, "invalid X-User-Id header");
            return false;
        }

        String username = request.getHeader(HEADER_USERNAME);
        String tokenId = request.getHeader(HEADER_TOKEN_ID);
        CurrentUserContext.set(new CurrentUser(
                userId,
                StringUtils.hasText(username) ? username : null,
                null,
                StringUtils.hasText(tokenId) ? tokenId : null,
                Set.of(),
                Set.of()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private boolean isTrustedGatewayRequest(HttpServletRequest request) {
        return StringUtils.hasText(gatewayInternalToken)
                && gatewayInternalToken.equals(request.getHeader(HEADER_GATEWAY_TOKEN));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(401, message)));
    }
}
