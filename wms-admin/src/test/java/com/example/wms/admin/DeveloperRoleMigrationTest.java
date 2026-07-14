package com.example.wms.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.admin.model.entity.SysRole;
import com.example.wms.admin.model.entity.SysRolePermission;
import com.example.wms.admin.model.mapper.SysPermissionMapper;
import com.example.wms.admin.model.mapper.SysRoleMapper;
import com.example.wms.admin.model.mapper.SysRolePermissionMapper;
import com.example.wms.common.enums.CommonStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Verifies V9__add_developer_role.sql: the DEVELOPER role exists, is enabled, and holds every permission. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true")
class DeveloperRoleMigrationTest {

    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysPermissionMapper sysPermissionMapper;
    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Test
    void developerRoleExistsAndIsEnabled() {
        SysRole role = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class).eq(SysRole::getRoleCode, "DEVELOPER"));
        assertNotNull(role, "V9 must seed the DEVELOPER role");
        assertEquals("开发者", role.getRoleName());
        assertEquals(CommonStatus.ENABLED, role.getStatus());
    }

    @Test
    void developerHasEveryExistingPermission() {
        SysRole role = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class).eq(SysRole::getRoleCode, "DEVELOPER"));
        assertNotNull(role);

        Long developerGrantCount = sysRolePermissionMapper.selectCount(
                Wrappers.lambdaQuery(SysRolePermission.class).eq(SysRolePermission::getRoleId, role.getId()));
        Long totalPermissionCount = sysPermissionMapper.selectCount(null);

        assertEquals(totalPermissionCount, developerGrantCount,
                "DEVELOPER must be granted every permission that currently exists (developer_permission_count == total_permission_count)");
    }
}
