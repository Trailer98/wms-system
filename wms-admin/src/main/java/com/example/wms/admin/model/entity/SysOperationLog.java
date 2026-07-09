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
    private Long userId;
    private String operator;
    private String operationType;
    private String module;
    private String bizNo;
    private String bizType;
    private Long bizId;
    private String content;
    private String requestUri;
    private String requestMethod;
    private boolean success = true;
    private String errorMessage;
    private String ip;
    private LocalDateTime createTime;

    public SysOperationLog(
            Long userId,
            String operator,
            String operationType,
            String module,
            String bizNo,
            String bizType,
            Long bizId,
            String content,
            String requestUri,
            String requestMethod,
            boolean success,
            String errorMessage,
            String ip
    ) {
        this.userId = userId;
        this.operator = operator;
        this.operationType = operationType;
        this.module = module;
        this.bizNo = bizNo;
        this.bizType = bizType;
        this.bizId = bizId;
        this.content = content;
        this.requestUri = requestUri;
        this.requestMethod = requestMethod;
        this.success = success;
        this.errorMessage = errorMessage;
        this.ip = ip;
        this.createTime = LocalDateTime.now();
    }
}
