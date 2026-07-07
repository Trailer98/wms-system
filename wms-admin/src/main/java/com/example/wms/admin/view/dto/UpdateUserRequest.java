package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 64) String realName,
        @Size(max = 32) String phone,
        @Size(max = 128) String email
) {
}
