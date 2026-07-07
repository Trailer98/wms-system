package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.SysUserService;
import com.example.wms.admin.view.dto.AssignRolesRequest;
import com.example.wms.admin.view.dto.CreateUserRequest;
import com.example.wms.admin.view.dto.ResetPasswordRequest;
import com.example.wms.admin.view.dto.RoleResponse;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateUserRequest;
import com.example.wms.admin.view.dto.UserQuery;
import com.example.wms.admin.view.dto.UserResponse;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/page")
    @RequiresPermission("user:view")
    public ApiResponse<PageResponse<UserResponse>> search(UserQuery query) {
        return ApiResponse.ok(sysUserService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("user:view")
    public ApiResponse<UserResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(sysUserService.getDetail(id));
    }

    @PostMapping
    @RequiresPermission("user:create")
    @SysOperationLog(operationType = "创建用户", content = "创建用户", module = "用户管理", bizNo = "#request.username()")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(sysUserService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("user:update")
    @SysOperationLog(operationType = "编辑用户", content = "编辑用户", module = "用户管理")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(sysUserService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @RequiresPermission("user:disable")
    @SysOperationLog(operationType = "切换用户状态", content = "切换用户状态", module = "用户管理")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(sysUserService.updateStatus(id, request));
    }

    @PostMapping("/{id}/reset-password")
    @RequiresPermission("user:update")
    @SysOperationLog(operationType = "重置密码", content = "重置用户密码", module = "用户管理")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable Long id, @RequestBody(required = false) ResetPasswordRequest request) {
        String newPassword = sysUserService.resetPassword(id, request != null ? request : new ResetPasswordRequest(null));
        return ApiResponse.ok(Map.of("newPassword", newPassword));
    }

    @GetMapping("/{id}/roles")
    @RequiresPermission("user:view")
    public ApiResponse<List<RoleResponse>> getRoles(@PathVariable Long id) {
        return ApiResponse.ok(sysUserService.getRoles(id));
    }

    @PostMapping("/{id}/roles")
    @RequiresPermission("user:update")
    @SysOperationLog(operationType = "分配用户角色", content = "分配用户角色", module = "用户管理")
    public ApiResponse<List<RoleResponse>> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.ok(sysUserService.assignRoles(id, request));
    }
}
