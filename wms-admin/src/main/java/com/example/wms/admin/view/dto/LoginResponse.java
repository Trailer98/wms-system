package com.example.wms.admin.view.dto;

import java.util.List;

public record LoginResponse(
        String token,
        UserResponse user,
        List<String> roles,
        List<String> permissions
) {
}
