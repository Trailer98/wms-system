package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.SysRole;
import com.example.wms.admin.model.entity.SysUser;
import com.example.wms.admin.model.entity.SysUserRole;
import com.example.wms.admin.model.mapper.SysRoleMapper;
import com.example.wms.admin.model.mapper.SysUserMapper;
import com.example.wms.admin.model.mapper.SysUserRoleMapper;
import com.example.wms.admin.security.PasswordEncoder;
import com.example.wms.admin.view.dto.AssignRolesRequest;
import com.example.wms.admin.view.dto.CreateUserRequest;
import com.example.wms.admin.view.dto.ResetPasswordRequest;
import com.example.wms.admin.view.dto.RoleResponse;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateUserRequest;
import com.example.wms.admin.view.dto.UserQuery;
import com.example.wms.admin.view.dto.UserResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.CommonStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysUserService {

    private static final String DEFAULT_RESET_PASSWORD = "Reset@123";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserService(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, request.username())) > 0) {
            throw new BusinessException("username already exists");
        }
        SysUser user = new SysUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.realName(),
                request.phone(),
                request.email()
        );
        sysUserMapper.insert(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        SysUser user = getById(id);
        user.update(request.realName(), request.phone(), request.email());
        sysUserMapper.updateById(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UpdateStatusRequest request) {
        SysUser user = getById(id);
        user.changeStatus(parseStatus(request.status()));
        sysUserMapper.updateById(user);
        return UserResponse.from(user);
    }

    @Transactional
    public String resetPassword(Long id, ResetPasswordRequest request) {
        SysUser user = getById(id);
        String newPassword = StringUtils.hasText(request.newPassword()) ? request.newPassword() : DEFAULT_RESET_PASSWORD;
        user.resetPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        return newPassword;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(UserQuery query) {
        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(SysUser.class)
                        .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                        .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                        .orderByDesc(SysUser::getCreateTime)
        );
        return PageResponse.from(page, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getDetail(Long id) {
        return UserResponse.from(getById(id));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(Long id) {
        getById(id);
        List<Long> roleIds = sysUserRoleMapper.selectList(Wrappers.lambdaQuery(SysUserRole.class)
                        .eq(SysUserRole::getUserId, id))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream().map(RoleResponse::from).toList();
    }

    @Transactional
    public List<RoleResponse> assignRoles(Long id, AssignRolesRequest request) {
        getById(id);
        sysUserRoleMapper.delete(Wrappers.lambdaQuery(SysUserRole.class).eq(SysUserRole::getUserId, id));
        for (Long roleId : request.roleIds()) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null) {
                throw new BusinessException("role not found: " + roleId);
            }
            sysUserRoleMapper.insert(new SysUserRole(id, roleId));
        }
        return getRoles(id);
    }

    private SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("user not found");
        }
        return user;
    }

    private CommonStatus parseStatus(String status) {
        try {
            return CommonStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("invalid user status: " + status);
        }
    }
}
