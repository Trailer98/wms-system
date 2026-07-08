package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSysDictItemRequest(
        @NotBlank @Size(max = 64) String dictCode,
        @NotBlank @Size(max = 128) String itemValue,
        @NotBlank @Size(max = 128) String itemLabel,
        String itemLabelEn,
        int sortOrder,
        String tagType,
        String cssClass,
        String remark
) {
}
