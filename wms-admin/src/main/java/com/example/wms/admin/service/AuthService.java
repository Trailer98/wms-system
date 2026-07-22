package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.admin.model.entity.SysPermission;
import com.example.wms.admin.model.entity.SysRole;
import com.example.wms.admin.model.entity.SysRolePermission;
import com.example.wms.admin.model.entity.SysUser;
import com.example.wms.admin.model.entity.SysUserRole;
import com.example.wms.admin.model.mapper.SysPermissionMapper;
import com.example.wms.admin.model.mapper.SysRoleMapper;
import com.example.wms.admin.model.mapper.SysRolePermissionMapper;
import com.example.wms.admin.model.mapper.SysUserMapper;
import com.example.wms.admin.model.mapper.SysUserRoleMapper;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.security.JwtTokenService;
import com.example.wms.admin.security.PasswordEncoder;
import com.example.wms.admin.view.dto.LoginRequest;
import com.example.wms.admin.view.dto.LoginResponse;
import com.example.wms.admin.view.dto.UserResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.UnauthorizedException;
import com.example.wms.common.enums.CommonStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Deprecated(forRemoval = false)
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysPermissionMapper sysPermissionMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysRolePermissionMapper sysRolePermissionMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("username or password is incorrect");
        }
        if (!user.isUsable()) {
            throw new BusinessException("account has been disabled");
        }

        Set<String> roleCodes = loadRoleCodes(user.getId());
        Set<String> permissionCodes = loadPermissionCodes(user.getId());

        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), user.getRealName(), roleCodes, permissionCodes);
        String token = jwtTokenService.generateToken(currentUser);

        user.recordLogin();
        sysUserMapper.updateById(user);

        return new LoginResponse(token, UserResponse.from(user), new ArrayList<>(roleCodes), new ArrayList<>(permissionCodes));
    }

    @Transactional(readOnly = true)
    public LoginResponse me() {
        CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser == null) {
            throw new UnauthorizedException("login required");
        }
        SysUser user = sysUserMapper.selectById(currentUser.userId());
        if (user == null) {
            throw new UnauthorizedException("user no longer exists");
        }
        return new LoginResponse(null, UserResponse.from(user), new ArrayList<>(currentUser.roleCodes()), new ArrayList<>(currentUser.permissionCodes()));
    }

    private Set<String> loadRoleCodes(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(Wrappers.lambdaQuery(SysUserRole.class)
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() == CommonStatus.ENABLED)
                .map(SysRole::getRoleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> loadPermissionCodes(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(Wrappers.lambdaQuery(SysUserRole.class)
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        List<Long> usableRoleIds = sysRoleMapper.selectBatchIds(roleIds).stream()
                .filter(role -> role.getStatus() == CommonStatus.ENABLED)
                .map(SysRole::getId)
                .toList();
        if (usableRoleIds.isEmpty()) {
            return Set.of();
        }

        List<Long> permissionIds = sysRolePermissionMapper.selectList(Wrappers.lambdaQuery(SysRolePermission.class)
                        .in(SysRolePermission::getRoleId, usableRoleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return Set.of();
        }

        return sysPermissionMapper.selectBatchIds(permissionIds).stream()
                .filter(permission -> permission.getStatus() == CommonStatus.ENABLED)
                .map(SysPermission::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
