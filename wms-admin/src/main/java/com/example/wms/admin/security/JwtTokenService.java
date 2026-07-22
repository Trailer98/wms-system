package com.example.wms.admin.security;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.RegisteredPayload;
import com.example.wms.common.common.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated WMS no longer parses local JWTs on protected requests. Gateway/auth-service validates
 * tokens and WMS receives trusted Gateway headers. Kept temporarily for the legacy local login
 * rollback path.
 *
 * Stateless JWT issuing/parsing on top of Hutool (already a project dependency), so no new library
 * (Spring Security / jjwt) is introduced just for login. Roles and permission codes are baked into
 * the token payload at login time; there is no server-side revocation list, so permission changes
 * only take effect the next time a user logs in.
 */
@Deprecated(forRemoval = false)
@Component
public class JwtTokenService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_REAL_NAME = "realName";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";

    private final byte[] secretKey;
    private final long expireMinutes;

    public JwtTokenService(
            @Value("${wms.jwt.secret:wms-system-default-jwt-secret-please-change}") String secret,
            @Value("${wms.jwt.expire-minutes:720}") long expireMinutes
    ) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expireMinutes = expireMinutes;
    }

    public String generateToken(CurrentUser currentUser) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(CLAIM_USER_ID, currentUser.userId());
        payload.put(CLAIM_USERNAME, currentUser.username());
        payload.put(CLAIM_REAL_NAME, currentUser.realName());
        payload.put(CLAIM_ROLES, String.join(",", currentUser.roleCodes()));
        payload.put(CLAIM_PERMISSIONS, String.join(",", currentUser.permissionCodes()));
        payload.put(RegisteredPayload.ISSUED_AT, new Date());
        payload.put(RegisteredPayload.EXPIRES_AT, Date.from(Instant.now().plus(expireMinutes, ChronoUnit.MINUTES)));
        return JWTUtil.createToken(payload, secretKey);
    }

    public CurrentUser parseToken(String token) {
        JWT jwt;
        try {
            jwt = JWTUtil.parseToken(token);
            jwt.setKey(secretKey);
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("invalid token");
        }
        if (!jwt.verify()) {
            throw new UnauthorizedException("invalid token signature");
        }
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException ex) {
            throw new UnauthorizedException("token has expired");
        }

        Number userId = (Number) jwt.getPayload(CLAIM_USER_ID);
        String username = (String) jwt.getPayload(CLAIM_USERNAME);
        String realName = (String) jwt.getPayload(CLAIM_REAL_NAME);
        Set<String> roles = splitToSet((String) jwt.getPayload(CLAIM_ROLES));
        Set<String> permissions = splitToSet((String) jwt.getPayload(CLAIM_PERMISSIONS));

        return new CurrentUser(userId != null ? userId.longValue() : null, username, realName, roles, permissions);
    }

    private Set<String> splitToSet(String value) {
        Set<String> result = new HashSet<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }
}
