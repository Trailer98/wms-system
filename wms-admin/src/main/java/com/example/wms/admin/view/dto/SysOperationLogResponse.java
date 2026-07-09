package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysOperationLog;

import java.time.LocalDateTime;

public record SysOperationLogResponse(
        Long id,
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
        String ip,
        LocalDateTime createTime
) {

    public static SysOperationLogResponse from(SysOperationLog log) {
        return new SysOperationLogResponse(
                log.getId(),
                log.getUserId(),
                log.getOperator(),
                log.getOperationType(),
                log.getModule(),
                log.getBizNo(),
                log.getBizType(),
                log.getBizId(),
                log.getContent(),
                log.getRequestUri(),
                log.getRequestMethod(),
                log.isSuccess(),
                log.getErrorMessage(),
                log.getIp(),
                log.getCreateTime()
        );
    }
}
