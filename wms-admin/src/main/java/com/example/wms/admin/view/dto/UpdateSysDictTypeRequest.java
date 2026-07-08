package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSysDictTypeRequest(
        @NotBlank @Size(max = 128) String dictName,
        String remark,
        int sortOrder
) {
}
