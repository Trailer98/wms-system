package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("sys_operation_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SysOperationLog {

    private Long id;
    private String operator;
    private String operationType;
    private String bizNo;
    private String content;
    private String ip;
    private LocalDateTime createTime;

    public SysOperationLog(String operator, String operationType, String bizNo, String content, String ip) {
        this.operator = operator;
        this.operationType = operationType;
        this.bizNo = bizNo;
        this.content = content;
        this.ip = ip;
        this.createTime = LocalDateTime.now();
    }
}
