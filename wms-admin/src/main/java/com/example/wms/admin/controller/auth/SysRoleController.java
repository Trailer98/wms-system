package com.example.wms.admin.controller.auth;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.SysRoleService;
import com.example.wms.admin.view.dto.AssignPermissionsRequest;
import com.example.wms.admin.view.dto.CreateRoleRequest;
import com.example.wms.admin.view.dto.PermissionResponse;
import com.example.wms.admin.view.dto.RoleQuery;
import com.example.wms.admin.view.dto.RoleResponse;
import com.example.wms.admin.view.dto.UpdateRoleRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
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

@RestController
@RequestMapping("/roles")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @GetMapping("/page")
    @RequiresPermission("role:view")
    public ApiResponse<PageResponse<RoleResponse>> search(RoleQuery query) {
        return ApiResponse.ok(sysRoleService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("role:view")
    public ApiResponse<RoleResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(sysRoleService.getDetail(id));
    }

    @PostMapping
    @RequiresPermission("role:create")
    @SysOperationLog(operationType = "创建角色", content = "创建角色", module = "角色管理", bizNo = "#request.roleCode()")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.ok(sysRoleService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("role:update")
    @SysOperationLog(operationType = "编辑角色", content = "编辑角色", module = "角色管理")
    public ApiResponse<RoleResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.ok(sysRoleService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @RequiresPermission("role:update")
    @SysOperationLog(operationType = "切换角色状态", content = "切换角色状态", module = "角色管理")
    public ApiResponse<RoleResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(sysRoleService.updateStatus(id, request));
    }

    @GetMapping("/{id}/permissions")
    @RequiresPermission("role:view")
    public ApiResponse<List<PermissionResponse>> getPermissions(@PathVariable Long id) {
        return ApiResponse.ok(sysRoleService.getPermissions(id));
    }

    @PostMapping("/{id}/permissions")
    @RequiresPermission("role:assign")
    @SysOperationLog(operationType = "分配角色权限", content = "分配角色权限", module = "角色管理")
    public ApiResponse<List<PermissionResponse>> assignPermissions(@PathVariable Long id, @Valid @RequestBody AssignPermissionsRequest request) {
        return ApiResponse.ok(sysRoleService.assignPermissions(id, request));
    }
}
