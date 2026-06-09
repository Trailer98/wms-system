package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/success")
    @SysOperationLog(operationType = "测试成功接口", content = "测试成功接口")
    public ApiResponse<String> success() {
        return ApiResponse.ok("success");
    }

    @GetMapping("/fail")
    @SysOperationLog(operationType = "测试失败接口", content = "测试失败接口")
    public ApiResponse<Void> fail() {
        throw new BusinessException("This is a test exception");
    }
}
