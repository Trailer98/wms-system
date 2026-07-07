package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.admin.model.entity.SysPermission;
import com.example.wms.admin.model.mapper.SysPermissionMapper;
import com.example.wms.admin.view.dto.PermissionResponse;
import com.example.wms.admin.view.dto.PermissionTreeNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysPermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    public SysPermissionService(SysPermissionMapper sysPermissionMapper) {
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> list() {
        return sysPermissionMapper.selectList(Wrappers.lambdaQuery(SysPermission.class)
                        .orderByAsc(SysPermission::getSort, SysPermission::getPermissionCode))
                .stream().map(PermissionResponse::from).toList();
    }

    /**
     * V1 keeps the permission table flat (no maintained parent_id hierarchy); this groups by the
     * code prefix before the first ':' purely so the role-assignment UI can render a tree.
     */
    @Transactional(readOnly = true)
    public List<PermissionTreeNode> tree() {
        Map<String, List<PermissionResponse>> byModule = new LinkedHashMap<>();
        for (PermissionResponse permission : list()) {
            String module = permission.permissionCode().contains(":")
                    ? permission.permissionCode().substring(0, permission.permissionCode().indexOf(':'))
                    : permission.permissionCode();
            byModule.computeIfAbsent(module, key -> new ArrayList<>()).add(permission);
        }

        return byModule.entrySet().stream()
                .map(entry -> new PermissionTreeNode(entry.getKey(), moduleName(entry.getKey()), entry.getValue()))
                .toList();
    }

    private String moduleName(String module) {
        return switch (module) {
            case "warehouse" -> "仓库管理";
            case "area" -> "库区管理";
            case "location" -> "库位管理";
            case "sku" -> "SKU管理";
            case "inbound" -> "入库管理";
            case "outbound" -> "出库管理";
            case "inventory" -> "库存管理";
            case "exception" -> "异常管理";
            case "user" -> "用户管理";
            case "role" -> "角色管理";
            case "permission" -> "权限管理";
            default -> module;
        };
    }
}
