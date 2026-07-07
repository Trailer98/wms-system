package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotBlank @Size(max = 64) String roleName,
        @Size(max = 255) String remark
) {
}
