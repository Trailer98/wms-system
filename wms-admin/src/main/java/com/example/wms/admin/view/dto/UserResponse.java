package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysUser;
import com.example.wms.common.enums.CommonStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String realName,
        String phone,
        String email,
        CommonStatus status,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    public static UserResponse from(SysUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getCreateTime(),
                user.getUpdateTime()
        );
    }
}
