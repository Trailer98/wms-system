package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@TableName("sys_user_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysUserRole {

    private Long id;
    private Long userId;
    private Long roleId;

    public SysUserRole(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }
}
