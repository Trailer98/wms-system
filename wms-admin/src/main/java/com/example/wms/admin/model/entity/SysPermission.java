package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.CommonStatus;
import com.example.wms.common.enums.PermissionType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("sys_permission")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysPermission {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private PermissionType permissionType;
    private Long parentId;
    private String path;
    private String method;
    private int sort;
    private CommonStatus status = CommonStatus.ENABLED;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public SysPermission(
            String permissionCode,
            String permissionName,
            PermissionType permissionType,
            Long parentId,
            String path,
            String method,
            int sort,
            String remark
    ) {
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.permissionType = permissionType;
        this.parentId = parentId;
        this.path = path;
        this.method = method;
        this.sort = sort;
        this.remark = remark;
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }
}
