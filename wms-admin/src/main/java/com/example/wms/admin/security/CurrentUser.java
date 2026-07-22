package com.example.wms.admin.security;

import java.util.Set;

public record CurrentUser(
        Long userId,
        String username,
        String realName,
        String tokenId,
        Set<String> roleCodes,
        Set<String> permissionCodes
) {

    public CurrentUser(Long userId, String username, String realName, Set<String> roleCodes, Set<String> permissionCodes) {
        this(userId, username, realName, null, roleCodes, permissionCodes);
    }

    public CurrentUser withAuthorizationContext(Set<String> roleCodes, Set<String> permissionCodes) {
        return new CurrentUser(userId, username, realName, tokenId, roleCodes, permissionCodes);
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCodes != null && permissionCodes.contains(permissionCode);
    }
}
