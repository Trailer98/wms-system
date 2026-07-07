package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.CommonStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("sys_role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysRole {

    public static final String ADMIN_ROLE_CODE = "ADMIN";

    private Long id;
    private String roleCode;
    private String roleName;
    private CommonStatus status = CommonStatus.ENABLED;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")
    private boolean deleted = false;

    public SysRole(String roleCode, String roleName, String remark) {
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.remark = remark;
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    public void update(String roleName, String remark) {
        this.roleName = roleName;
        this.remark = remark;
        this.updateTime = LocalDateTime.now();
    }

    public void changeStatus(CommonStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public boolean isUsable() {
        return status == CommonStatus.ENABLED;
    }
}
