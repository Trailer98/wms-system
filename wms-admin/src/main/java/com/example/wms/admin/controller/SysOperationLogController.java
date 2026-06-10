package com.example.wms.admin.controller;

import com.example.wms.admin.service.SysOperationLogService;
import com.example.wms.common.common.PageResponse;
import com.example.wms.admin.view.dto.SysOperationLogQuery;
import com.example.wms.admin.view.dto.SysOperationLogResponse;
import com.example.wms.common.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operation-logs")
public class SysOperationLogController {

    private final SysOperationLogService sysOperationLogService;

    public SysOperationLogController(SysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SysOperationLogResponse>> search(SysOperationLogQuery query) {
        return ApiResponse.ok(sysOperationLogService.search(query));
    }
}
