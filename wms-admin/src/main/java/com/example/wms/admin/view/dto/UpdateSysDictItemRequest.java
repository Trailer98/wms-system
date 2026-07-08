package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code dictCode}/{@code itemValue} are deliberately not editable here — the pair anchors every
 * usage of this item (business logic keys off {@code itemValue}, the dictionary just relabels it), so
 * changing it in place would silently detach existing lookups. Delete and recreate instead.
 */
public record UpdateSysDictItemRequest(
        @NotBlank @Size(max = 128) String itemLabel,
        String itemLabelEn,
        int sortOrder,
        String tagType,
        String cssClass,
        String remark
) {
}
