package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@TableName("sys_role_permission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysRolePermission {

    private Long id;
    private Long roleId;
    private Long permissionId;

    public SysRolePermission(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }
}
