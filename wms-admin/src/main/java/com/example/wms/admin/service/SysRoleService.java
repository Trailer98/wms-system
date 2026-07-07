package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.SysPermission;
import com.example.wms.admin.model.entity.SysRole;
import com.example.wms.admin.model.entity.SysRolePermission;
import com.example.wms.admin.model.mapper.SysPermissionMapper;
import com.example.wms.admin.model.mapper.SysRoleMapper;
import com.example.wms.admin.model.mapper.SysRolePermissionMapper;
import com.example.wms.admin.view.dto.AssignPermissionsRequest;
import com.example.wms.admin.view.dto.CreateRoleRequest;
import com.example.wms.admin.view.dto.PermissionResponse;
import com.example.wms.admin.view.dto.RoleQuery;
import com.example.wms.admin.view.dto.RoleResponse;
import com.example.wms.admin.view.dto.UpdateRoleRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.CommonStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public SysRoleService(
            SysRoleMapper sysRoleMapper,
            SysPermissionMapper sysPermissionMapper,
            SysRolePermissionMapper sysRolePermissionMapper
    ) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        if (sysRoleMapper.selectCount(Wrappers.lambdaQuery(SysRole.class)
                .eq(SysRole::getRoleCode, request.roleCode())) > 0) {
            throw new BusinessException("role code already exists");
        }
        SysRole role = new SysRole(request.roleCode(), request.roleName(), request.remark());
        sysRoleMapper.insert(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse update(Long id, UpdateRoleRequest request) {
        SysRole role = getById(id);
        role.update(request.roleName(), request.remark());
        sysRoleMapper.updateById(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse updateStatus(Long id, UpdateStatusRequest request) {
        SysRole role = getById(id);
        CommonStatus status = parseStatus(request.status());
        if (SysRole.ADMIN_ROLE_CODE.equals(role.getRoleCode()) && status == CommonStatus.DISABLED) {
            throw new BusinessException("the ADMIN role cannot be disabled");
        }
        role.changeStatus(status);
        sysRoleMapper.updateById(role);
        return RoleResponse.from(role);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> search(RoleQuery query) {
        Page<SysRole> page = sysRoleMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(SysRole.class)
                        .like(StringUtils.hasText(query.getRoleCode()), SysRole::getRoleCode, query.getRoleCode())
                        .like(StringUtils.hasText(query.getRoleName()), SysRole::getRoleName, query.getRoleName())
                        .eq(query.getStatus() != null, SysRole::getStatus, query.getStatus())
                        .orderByDesc(SysRole::getCreateTime)
        );
        return PageResponse.from(page, RoleResponse::from);
    }

    @Transactional(readOnly = true)
    public RoleResponse getDetail(Long id) {
        return RoleResponse.from(getById(id));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions(Long id) {
        getById(id);
        List<Long> permissionIds = sysRolePermissionMapper.selectList(Wrappers.lambdaQuery(SysRolePermission.class)
                        .eq(SysRolePermission::getRoleId, id))
                .stream().map(SysRolePermission::getPermissionId).toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return sysPermissionMapper.selectBatchIds(permissionIds).stream().map(PermissionResponse::from).toList();
    }

    @Transactional
    public List<PermissionResponse> assignPermissions(Long id, AssignPermissionsRequest request) {
        getById(id);
        sysRolePermissionMapper.delete(Wrappers.lambdaQuery(SysRolePermission.class).eq(SysRolePermission::getRoleId, id));
        for (Long permissionId : request.permissionIds()) {
            SysPermission permission = sysPermissionMapper.selectById(permissionId);
            if (permission == null) {
                throw new BusinessException("permission not found: " + permissionId);
            }
            sysRolePermissionMapper.insert(new SysRolePermission(id, permissionId));
        }
        return getPermissions(id);
    }

    private SysRole getById(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("role not found");
        }
        return role;
    }

    private CommonStatus parseStatus(String status) {
        try {
            return CommonStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("invalid role status: " + status);
        }
    }
}
