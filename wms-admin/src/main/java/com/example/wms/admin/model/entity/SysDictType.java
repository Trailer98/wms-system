package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.CommonStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("sys_dict_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysDictType {

    private Long id;
    private String dictCode;
    private String dictName;
    private CommonStatus status = CommonStatus.ENABLED;
    private String remark;
    private int sortOrder;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public SysDictType(String dictCode, String dictName, String remark, int sortOrder, String createdBy) {
        this.dictCode = dictCode;
        this.dictName = dictName;
        this.remark = remark;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String dictName, String remark, int sortOrder, String updatedBy) {
        this.dictName = dictName;
        this.remark = remark;
        this.sortOrder = sortOrder;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(CommonStatus status, String updatedBy) {
        this.status = status;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
