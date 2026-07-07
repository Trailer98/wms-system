package com.example.wms.admin.security;

import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over Hutool's BCrypt (already a project dependency) so password hashing has one
 * call site instead of scattering {@code BCrypt.hashpw}/{@code checkpw} across services.
 */
@Component
public class PasswordEncoder {

    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
