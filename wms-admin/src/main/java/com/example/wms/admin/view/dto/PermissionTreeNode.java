package com.example.wms.admin.view.dto;

import java.util.List;

/**
 * V1 has no real parent_id hierarchy in the permission table (flat list, seeded once); this node
 * groups permissions by their code prefix (e.g. "warehouse" out of "warehouse:view") purely for
 * display in the role-assignment permission tree.
 */
public record PermissionTreeNode(
        String module,
        String moduleName,
        List<PermissionResponse> permissions
) {
}
