package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSysDictTypeRequest(
        @NotBlank @Size(max = 64) String dictCode,
        @NotBlank @Size(max = 128) String dictName,
        String remark,
        int sortOrder
) {
}
