package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysDictItem;

import java.time.LocalDateTime;

public record SysDictItemResponse(
        Long id,
        String dictCode,
        String itemValue,
        String itemLabel,
        String itemLabelEn,
        int sortOrder,
        String status,
        String tagType,
        String cssClass,
        boolean isSystem,
        String remark,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static SysDictItemResponse from(SysDictItem item) {
        return new SysDictItemResponse(
                item.getId(),
                item.getDictCode(),
                item.getItemValue(),
                item.getItemLabel(),
                item.getItemLabelEn(),
                item.getSortOrder(),
                item.getStatus().name(),
                item.getTagType(),
                item.getCssClass(),
                item.isSystem(),
                item.getRemark(),
                item.getCreatedBy(),
                item.getCreatedAt(),
                item.getUpdatedBy(),
                item.getUpdatedAt()
        );
    }
}
