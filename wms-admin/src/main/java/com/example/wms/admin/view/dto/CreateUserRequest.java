package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 6, max = 64) String password,
        @Size(max = 64) String realName,
        @Size(max = 32) String phone,
        @Size(max = 128) String email
) {
}
