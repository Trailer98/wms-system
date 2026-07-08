package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysDictItem;

/**
 * The lightweight shape business pages actually need to render a label/tag — deliberately narrower
 * than {@link SysDictItemResponse} (which is the full admin-editing shape), returned by the
 * batch/items lookup endpoints business pages call.
 */
public record SysDictItemView(
        String value,
        String label,
        int sortOrder,
        String tagType,
        String status
) {
    public static SysDictItemView from(SysDictItem item) {
        return new SysDictItemView(
                item.getItemValue(),
                item.getItemLabel(),
                item.getSortOrder(),
                item.getTagType(),
                item.getStatus().name()
        );
    }
}
