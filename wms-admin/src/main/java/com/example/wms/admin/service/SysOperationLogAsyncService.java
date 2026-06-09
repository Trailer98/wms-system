package com.example.wms.admin.service;

import com.example.wms.admin.model.entity.SysOperationLog;
import com.example.wms.admin.model.mapper.SysOperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysOperationLogAsyncService {

    private static final Logger log = LoggerFactory.getLogger(SysOperationLogAsyncService.class);

    private final SysOperationLogMapper sysOperationLogMapper;

    public SysOperationLogAsyncService(SysOperationLogMapper sysOperationLogMapper) {
        this.sysOperationLogMapper = sysOperationLogMapper;
    }

    @Async("operationLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(String operationType, String bizNo, String content, String ip) {
        try {
            SysOperationLog operationLog = new SysOperationLog(
                    "admin",
                    operationType,
                    bizNo,
                    content,
                    ip
            );

            sysOperationLogMapper.insert(operationLog);
        } catch (RuntimeException ex) {
            log.error("save operation log failed, operationType={}, bizNo={}", operationType, bizNo, ex);
        }
    }
}
