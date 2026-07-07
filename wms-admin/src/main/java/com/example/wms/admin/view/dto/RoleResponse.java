package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysRole;
import com.example.wms.common.enums.CommonStatus;

import java.time.LocalDateTime;

public record RoleResponse(
        Long id,
        String roleCode,
        String roleName,
        CommonStatus status,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    public static RoleResponse from(SysRole role) {
        return new RoleResponse(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getStatus(),
                role.getRemark(),
                role.getCreateTime(),
                role.getUpdateTime()
        );
    }
}
