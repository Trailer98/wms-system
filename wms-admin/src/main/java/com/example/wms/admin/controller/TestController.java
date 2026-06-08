package com.example.wms.admin.controller;

import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/success")
    public ApiResponse<String> success() {
        return ApiResponse.ok("success");
    }

    @GetMapping("/fail")
    public ApiResponse<Void> fail() {
        throw new BusinessException("This is a test exception");
    }
}
