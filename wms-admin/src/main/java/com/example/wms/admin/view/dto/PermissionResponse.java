package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysPermission;
import com.example.wms.common.enums.CommonStatus;
import com.example.wms.common.enums.PermissionType;

public record PermissionResponse(
        Long id,
        String permissionCode,
        String permissionName,
        PermissionType permissionType,
        Long parentId,
        String path,
        String method,
        int sort,
        CommonStatus status,
        String remark
) {

    public static PermissionResponse from(SysPermission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getPermissionType(),
                permission.getParentId(),
                permission.getPath(),
                permission.getMethod(),
                permission.getSort(),
                permission.getStatus(),
                permission.getRemark()
        );
    }
}
