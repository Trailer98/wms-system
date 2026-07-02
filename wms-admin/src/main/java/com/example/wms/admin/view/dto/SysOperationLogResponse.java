package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.SysOperationLog;

import java.time.LocalDateTime;

public record SysOperationLogResponse(
        Long id,
        String operator,
        String operationType,
        String bizNo,
        String content,
        String ip,
        LocalDateTime createTime
) {

    public static SysOperationLogResponse from(SysOperationLog log) {
        return new SysOperationLogResponse(
                log.getId(),
                log.getOperator(),
                log.getOperationType(),
                log.getBizNo(),
                log.getContent(),
                log.getIp(),
                log.getCreateTime()
        );
    }
}
