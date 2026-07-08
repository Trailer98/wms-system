package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysDictType;

import java.time.LocalDateTime;

public record SysDictTypeResponse(
        Long id,
        String dictCode,
        String dictName,
        String status,
        String remark,
        int sortOrder,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static SysDictTypeResponse from(SysDictType type) {
        return new SysDictTypeResponse(
                type.getId(),
                type.getDictCode(),
                type.getDictName(),
                type.getStatus().name(),
                type.getRemark(),
                type.getSortOrder(),
                type.getCreatedBy(),
                type.getCreatedAt(),
                type.getUpdatedBy(),
                type.getUpdatedAt()
        );
    }
}
