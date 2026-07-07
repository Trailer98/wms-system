package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.CommonStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("sys_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysUser {

    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String email;
    private CommonStatus status = CommonStatus.ENABLED;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")
    private boolean deleted = false;

    public SysUser(String username, String passwordHash, String realName, String phone, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.realName = realName;
        this.phone = phone;
        this.email = email;
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    public void update(String realName, String phone, String email) {
        this.realName = realName;
        this.phone = phone;
        this.email = email;
        this.updateTime = LocalDateTime.now();
    }

    public void changeStatus(CommonStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public void resetPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updateTime = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginTime = LocalDateTime.now();
    }

    public boolean isUsable() {
        return status == CommonStatus.ENABLED;
    }
}
