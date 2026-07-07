package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.WmsExceptionEventService;
import com.example.wms.admin.view.dto.WmsExceptionEventQuery;
import com.example.wms.admin.view.dto.WmsExceptionEventResponse;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms-exceptions")
public class WmsExceptionEventController {

    private final WmsExceptionEventService wmsExceptionEventService;

    public WmsExceptionEventController(WmsExceptionEventService wmsExceptionEventService) {
        this.wmsExceptionEventService = wmsExceptionEventService;
    }

    @GetMapping("/page")
    @RequiresPermission("exception:view")
    public ApiResponse<PageResponse<WmsExceptionEventResponse>> search(WmsExceptionEventQuery query) {
        return ApiResponse.ok(wmsExceptionEventService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("exception:view")
    public ApiResponse<WmsExceptionEventResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(wmsExceptionEventService.getDetail(id));
    }

    @PatchMapping("/{id}/handled")
    @RequiresPermission("exception:handle")
    @SysOperationLog(operationType = "标记异常事件已处理", content = "标记异常事件已处理", module = "异常管理")
    public ApiResponse<WmsExceptionEventResponse> markHandled(@PathVariable Long id) {
        return ApiResponse.ok(wmsExceptionEventService.markHandled(id));
    }
}
