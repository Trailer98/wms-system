package com.example.wms.admin.security;

import java.util.Set;

public record CurrentUser(
        Long userId,
        String username,
        String realName,
        Set<String> roleCodes,
        Set<String> permissionCodes
) {

    public boolean hasPermission(String permissionCode) {
        return permissionCodes != null && permissionCodes.contains(permissionCode);
    }
}
