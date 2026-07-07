package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.service.SysPermissionService;
import com.example.wms.admin.view.dto.PermissionResponse;
import com.example.wms.admin.view.dto.PermissionTreeNode;
import com.example.wms.common.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    public SysPermissionController(SysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    @GetMapping("/tree")
    @RequiresPermission("permission:view")
    public ApiResponse<List<PermissionTreeNode>> tree() {
        return ApiResponse.ok(sysPermissionService.tree());
    }

    @GetMapping("/list")
    @RequiresPermission("permission:view")
    public ApiResponse<List<PermissionResponse>> list() {
        return ApiResponse.ok(sysPermissionService.list());
    }
}
