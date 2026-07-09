package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.SysOperationLog;
import com.example.wms.admin.model.mapper.SysOperationLogMapper;
import com.example.wms.admin.view.dto.SysOperationLogQuery;
import com.example.wms.common.common.PageResponse;
import com.example.wms.admin.view.dto.SysOperationLogResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SysOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    public SysOperationLogService(SysOperationLogMapper sysOperationLogMapper) {
        this.sysOperationLogMapper = sysOperationLogMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<SysOperationLogResponse> search(SysOperationLogQuery query) {
        Page<SysOperationLog> page = sysOperationLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(SysOperationLog.class)
                        .like(StringUtils.hasText(query.getOperator()), SysOperationLog::getOperator, query.getOperator())
                        .like(StringUtils.hasText(query.getOperationType()), SysOperationLog::getOperationType, query.getOperationType())
                        .eq(StringUtils.hasText(query.getBizNo()), SysOperationLog::getBizNo, query.getBizNo())
                        .eq(StringUtils.hasText(query.getBizType()), SysOperationLog::getBizType, query.getBizType())
                        .ge(query.getStartTime() != null, SysOperationLog::getCreateTime, query.getStartTime())
                        .le(query.getEndTime() != null, SysOperationLog::getCreateTime, query.getEndTime())
                        .orderByDesc(SysOperationLog::getCreateTime)
        );

        return PageResponse.from(page, SysOperationLogResponse::from);
    }
}
