package com.example.wms.admin.config;

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
import com.example.wms.admin.security.PasswordEncoder;
import com.example.wms.common.enums.PermissionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Idempotently seeds the RBAC baseline (permission points, default roles, role-permission
 * mappings and the initial admin/admin123 account) on every startup. Each step checks for
 * existing data first, so re-running on restart never duplicates rows and never clobbers
 * permission grants an operator has since customized by hand through the role-management UI.
 * <p>
 * DISABLED BY DEFAULT: this RBAC baseline now lives in Flyway migration
 * {@code db/migration/V2__system_base_data.sql}, which is the single source of truth for system
 * base data. This runner is kept (not deleted) as an escape hatch and as the authoritative
 * reference for what V2 must contain; re-enable it only with {@code wms.legacy-java-seeding.enabled=true}
 * if you ever need the old startup-time seeding back. Leaving both on would double-seed (harmlessly,
 * since both are idempotent), but Flyway is intended to own this data now.
 */
@Component
@ConditionalOnProperty(name = "wms.legacy-java-seeding.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin123";

    private record PermissionSeed(String code, String name, PermissionType type, int sort) {
    }

    private static final List<PermissionSeed> PERMISSION_SEEDS = List.of(
            new PermissionSeed("warehouse:view", "仓库查看", PermissionType.MENU, 10),
            new PermissionSeed("warehouse:create", "新增仓库", PermissionType.BUTTON, 11),
            new PermissionSeed("warehouse:update", "编辑仓库", PermissionType.BUTTON, 12),
            new PermissionSeed("warehouse:disable", "停用仓库", PermissionType.BUTTON, 13),
            new PermissionSeed("area:view", "库区查看", PermissionType.MENU, 20),
            new PermissionSeed("area:create", "新增库区", PermissionType.BUTTON, 21),
            new PermissionSeed("area:update", "编辑库区", PermissionType.BUTTON, 22),
            new PermissionSeed("area:disable", "停用库区", PermissionType.BUTTON, 23),
            new PermissionSeed("location:view", "库位查看", PermissionType.MENU, 30),
            new PermissionSeed("location:create", "新增库位", PermissionType.BUTTON, 31),
            new PermissionSeed("location:update", "编辑库位", PermissionType.BUTTON, 32),
            new PermissionSeed("location:disable", "停用库位", PermissionType.BUTTON, 33),
            new PermissionSeed("sku:view", "SKU查看", PermissionType.MENU, 40),
            new PermissionSeed("sku:create", "新增SKU", PermissionType.BUTTON, 41),
            new PermissionSeed("sku:update", "编辑SKU", PermissionType.BUTTON, 42),
            new PermissionSeed("sku:disable", "停用SKU", PermissionType.BUTTON, 43),
            new PermissionSeed("inbound:view", "入库查看", PermissionType.MENU, 50),
            new PermissionSeed("inbound:create", "创建入库单", PermissionType.BUTTON, 51),
            new PermissionSeed("inbound:update", "编辑入库单", PermissionType.BUTTON, 52),
            new PermissionSeed("inbound:complete", "完成入库", PermissionType.BUTTON, 53),
            new PermissionSeed("inbound:cancel", "取消/删除入库单", PermissionType.BUTTON, 54),
            new PermissionSeed("outbound:view", "出库查看", PermissionType.MENU, 60),
            new PermissionSeed("outbound:create", "创建出库单", PermissionType.BUTTON, 61),
            new PermissionSeed("outbound:lock", "出库锁库", PermissionType.BUTTON, 62),
            new PermissionSeed("outbound:confirm", "确认出库", PermissionType.BUTTON, 63),
            new PermissionSeed("outbound:cancel", "取消出库", PermissionType.BUTTON, 64),
            new PermissionSeed("outbound:allocation:view", "出库分配明细查看", PermissionType.BUTTON, 65),
            new PermissionSeed("inventory:view", "库存查看", PermissionType.MENU, 70),
            new PermissionSeed("inventory:transaction:view", "库存流水查看", PermissionType.MENU, 71),
            new PermissionSeed("inventory:adjust", "库存调整", PermissionType.BUTTON, 72),
            new PermissionSeed("inventory:count", "库存盘点", PermissionType.BUTTON, 73),
            new PermissionSeed("exception:view", "异常查看", PermissionType.MENU, 80),
            new PermissionSeed("exception:handle", "异常处理", PermissionType.BUTTON, 81),
            new PermissionSeed("user:view", "用户查看", PermissionType.MENU, 90),
            new PermissionSeed("user:create", "新增用户", PermissionType.BUTTON, 91),
            new PermissionSeed("user:update", "编辑用户", PermissionType.BUTTON, 92),
            new PermissionSeed("user:disable", "停用用户", PermissionType.BUTTON, 93),
            new PermissionSeed("role:view", "角色查看", PermissionType.MENU, 100),
            new PermissionSeed("role:create", "新增角色", PermissionType.BUTTON, 101),
            new PermissionSeed("role:update", "编辑角色", PermissionType.BUTTON, 102),
            new PermissionSeed("role:assign", "分配角色权限", PermissionType.BUTTON, 103),
            new PermissionSeed("permission:view", "权限查看", PermissionType.MENU, 110),
            new PermissionSeed("permission:assign", "权限分配", PermissionType.BUTTON, 111),
            new PermissionSeed("stock-adjust:view", "库存调整查看", PermissionType.MENU, 120),
            new PermissionSeed("stock-adjust:create", "创建库存调整单", PermissionType.BUTTON, 121),
            new PermissionSeed("stock-adjust:update", "编辑库存调整单", PermissionType.BUTTON, 122),
            new PermissionSeed("stock-adjust:submit", "提交库存调整单", PermissionType.BUTTON, 123),
            new PermissionSeed("stock-adjust:confirm", "确认库存调整单", PermissionType.BUTTON, 124),
            new PermissionSeed("stock-adjust:cancel", "取消库存调整单", PermissionType.BUTTON, 125),
            new PermissionSeed("stock-count:view", "库存盘点查看", PermissionType.MENU, 130),
            new PermissionSeed("stock-count:create", "创建库存盘点任务", PermissionType.BUTTON, 131),
            new PermissionSeed("stock-count:start", "开始库存盘点", PermissionType.BUTTON, 132),
            new PermissionSeed("stock-count:record", "录入盘点实盘数量", PermissionType.BUTTON, 133),
            new PermissionSeed("stock-count:complete", "完成库存盘点", PermissionType.BUTTON, 134),
            new PermissionSeed("stock-count:cancel", "取消库存盘点", PermissionType.BUTTON, 135),
            new PermissionSeed("customer:view", "客户查看", PermissionType.MENU, 140),
            new PermissionSeed("customer:create", "新增客户", PermissionType.BUTTON, 141),
            new PermissionSeed("customer:update", "编辑客户", PermissionType.BUTTON, 142),
            new PermissionSeed("customer:disable", "停用客户", PermissionType.BUTTON, 143),
            new PermissionSeed("supplier:view", "供应商查看", PermissionType.MENU, 150),
            new PermissionSeed("supplier:create", "新增供应商", PermissionType.BUTTON, 151),
            new PermissionSeed("supplier:update", "编辑供应商", PermissionType.BUTTON, 152),
            new PermissionSeed("supplier:disable", "停用供应商", PermissionType.BUTTON, 153),
            new PermissionSeed("operation-log:view", "操作日志查看", PermissionType.MENU, 160),
            new PermissionSeed("sys-dict:view", "数据字典查看", PermissionType.MENU, 170),
            new PermissionSeed("sys-dict:create", "新增字典", PermissionType.BUTTON, 171),
            new PermissionSeed("sys-dict:update", "编辑字典", PermissionType.BUTTON, 172),
            new PermissionSeed("sys-dict:disable", "启停字典", PermissionType.BUTTON, 173),
            new PermissionSeed("sys-dict:delete", "删除字典项", PermissionType.BUTTON, 174)
    );

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            SysPermissionMapper sysPermissionMapper,
            SysRoleMapper sysRoleMapper,
            SysRolePermissionMapper sysRolePermissionMapper,
            SysUserMapper sysUserMapper,
            SysUserRoleMapper sysUserRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // Permission codes added in this rollout (stock-adjust/stock-count/customer/supplier/operation-log).
    // assignIfEmpty() below only fires for a role with zero grants, so it won't reach roles that were
    // already seeded by a prior startup (true for any long-lived environment). ensureNewPermissionGrants()
    // is the idempotent top-up for that case: it checks each (role, permission) pair individually and only
    // inserts what's missing, so it never touches grants an operator has since customized by hand.
    private static final Set<String> NEW_ADJUST_AND_COUNT_CODES = Set.of(
            "stock-adjust:view", "stock-adjust:create", "stock-adjust:update", "stock-adjust:submit", "stock-adjust:confirm", "stock-adjust:cancel",
            "stock-count:view", "stock-count:create", "stock-count:start", "stock-count:record", "stock-count:complete", "stock-count:cancel",
            "customer:view", "customer:create", "customer:update", "customer:disable",
            "supplier:view", "supplier:create", "supplier:update", "supplier:disable",
            "operation-log:view"
    );

    // sys-dict:view is granted to every seeded role (including WAREHOUSE_OPERATOR, which otherwise
    // gets none of the other sys-dict codes): this RBAC model has no "authenticated but no specific
    // permission" tier, and every role that can see a stock-movement list needs to resolve its
    // operationType/bizType labels through /sys-dicts/batch, so view access has to be universal.
    // Write access (create/update/disable/delete) stays admin-only except WAREHOUSE_MANAGER, which
    // per product decision gets view only, not management.
    private static final Set<String> NEW_SYS_DICT_ADMIN_CODES = Set.of(
            "sys-dict:view", "sys-dict:create", "sys-dict:update", "sys-dict:disable", "sys-dict:delete"
    );
    private static final Set<String> NEW_SYS_DICT_VIEW_ONLY_CODES = Set.of("sys-dict:view");

    private static final Set<String> NEW_WAREHOUSE_OPERATOR_CODES = Set.of(
            "stock-adjust:view", "stock-adjust:create", "stock-adjust:update", "stock-adjust:submit",
            "stock-count:view", "stock-count:create", "stock-count:start", "stock-count:record",
            "customer:view", "supplier:view"
    );

    private static final Set<String> NEW_INVENTORY_VIEWER_CODES = Set.of(
            "stock-adjust:view", "stock-count:view", "customer:view", "supplier:view"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, SysPermission> permissionsByCode = seedPermissions();
        Map<String, SysRole> rolesByCode = seedRoles();
        seedRolePermissions(rolesByCode, permissionsByCode);
        ensureNewPermissionGrants(rolesByCode, permissionsByCode);
        seedAdminUser(rolesByCode);
    }

    private void ensureNewPermissionGrants(Map<String, SysRole> roles, Map<String, SysPermission> permissions) {
        ensureGranted(roles.get("ADMIN"), NEW_ADJUST_AND_COUNT_CODES, permissions);
        ensureGranted(roles.get("WAREHOUSE_MANAGER"), NEW_ADJUST_AND_COUNT_CODES, permissions);
        ensureGranted(roles.get("WAREHOUSE_OPERATOR"), NEW_WAREHOUSE_OPERATOR_CODES, permissions);
        ensureGranted(roles.get("INVENTORY_VIEWER"), NEW_INVENTORY_VIEWER_CODES, permissions);

        ensureGranted(roles.get("ADMIN"), NEW_SYS_DICT_ADMIN_CODES, permissions);
        ensureGranted(roles.get("WAREHOUSE_MANAGER"), NEW_SYS_DICT_VIEW_ONLY_CODES, permissions);
        ensureGranted(roles.get("WAREHOUSE_OPERATOR"), NEW_SYS_DICT_VIEW_ONLY_CODES, permissions);
        ensureGranted(roles.get("INVENTORY_VIEWER"), NEW_SYS_DICT_VIEW_ONLY_CODES, permissions);
    }

    private void ensureGranted(SysRole role, Collection<String> codes, Map<String, SysPermission> permissions) {
        if (role == null) {
            return;
        }
        for (String code : codes) {
            SysPermission permission = permissions.get(code);
            if (permission == null) {
                log.warn("skip granting unknown permission code {} to role {}", code, role.getRoleCode());
                continue;
            }
            long exists = sysRolePermissionMapper.selectCount(Wrappers.lambdaQuery(SysRolePermission.class)
                    .eq(SysRolePermission::getRoleId, role.getId())
                    .eq(SysRolePermission::getPermissionId, permission.getId()));
            if (exists == 0) {
                sysRolePermissionMapper.insert(new SysRolePermission(role.getId(), permission.getId()));
            }
        }
    }

    private Map<String, SysPermission> seedPermissions() {
        Map<String, SysPermission> result = new LinkedHashMap<>();
        for (PermissionSeed seed : PERMISSION_SEEDS) {
            SysPermission existing = sysPermissionMapper.selectOne(Wrappers.lambdaQuery(SysPermission.class)
                    .eq(SysPermission::getPermissionCode, seed.code()));
            if (existing == null) {
                SysPermission permission = new SysPermission(seed.code(), seed.name(), seed.type(), null, null, null, seed.sort(), null);
                sysPermissionMapper.insert(permission);
                result.put(seed.code(), permission);
            } else {
                result.put(seed.code(), existing);
            }
        }
        return result;
    }

    private Map<String, SysRole> seedRoles() {
        Map<String, SysRole> result = new LinkedHashMap<>();
        result.put("ADMIN", ensureRole("ADMIN", "系统管理员", "拥有全部权限"));
        result.put("WAREHOUSE_MANAGER", ensureRole("WAREHOUSE_MANAGER", "仓库主管", "拥有全部仓储业务权限，不含用户与角色管理"));
        result.put("WAREHOUSE_OPERATOR", ensureRole("WAREHOUSE_OPERATOR", "仓管员", "负责日常入库、出库操作"));
        result.put("INVENTORY_VIEWER", ensureRole("INVENTORY_VIEWER", "库存查看员", "只读查看库存、流水与单据"));
        return result;
    }

    private SysRole ensureRole(String code, String name, String remark) {
        SysRole existing = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class).eq(SysRole::getRoleCode, code));
        if (existing != null) {
            return existing;
        }
        SysRole role = new SysRole(code, name, remark);
        sysRoleMapper.insert(role);
        return role;
    }

    private void seedRolePermissions(Map<String, SysRole> roles, Map<String, SysPermission> permissions) {
        assignIfEmpty(roles.get("ADMIN"), permissions.keySet(), permissions);
        assignIfEmpty(roles.get("WAREHOUSE_MANAGER"), warehouseManagerCodes(), permissions);
        assignIfEmpty(roles.get("WAREHOUSE_OPERATOR"), warehouseOperatorCodes(), permissions);
        assignIfEmpty(roles.get("INVENTORY_VIEWER"), inventoryViewerCodes(), permissions);
    }

    private Set<String> warehouseManagerCodes() {
        return PERMISSION_SEEDS.stream()
                .map(PermissionSeed::code)
                .filter(code -> !code.startsWith("user:") && !code.startsWith("role:") && !code.startsWith("permission:"))
                // WAREHOUSE_MANAGER gets sys-dict:view like every other role, but not the management codes.
                .filter(code -> !code.startsWith("sys-dict:") || code.equals("sys-dict:view"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> warehouseOperatorCodes() {
        return Set.of(
                "warehouse:view", "area:view", "location:view", "sku:view",
                "inbound:view", "inbound:create", "inbound:update", "inbound:complete", "inbound:cancel",
                "outbound:view", "outbound:create", "outbound:lock", "outbound:confirm", "outbound:cancel", "outbound:allocation:view",
                "inventory:view", "inventory:transaction:view",
                "exception:view",
                "stock-adjust:view", "stock-adjust:create", "stock-adjust:update", "stock-adjust:submit",
                "stock-count:view", "stock-count:create", "stock-count:start", "stock-count:record",
                "customer:view", "supplier:view",
                "sys-dict:view"
        );
    }

    private Set<String> inventoryViewerCodes() {
        return Set.of(
                "warehouse:view", "area:view", "location:view", "sku:view",
                "inbound:view", "outbound:view",
                "inventory:view", "inventory:transaction:view",
                "exception:view",
                "stock-adjust:view", "stock-count:view",
                "customer:view", "supplier:view",
                "sys-dict:view"
        );
    }

    private void assignIfEmpty(SysRole role, Collection<String> codes, Map<String, SysPermission> permissions) {
        if (role == null) {
            return;
        }
        long existingCount = sysRolePermissionMapper.selectCount(Wrappers.lambdaQuery(SysRolePermission.class)
                .eq(SysRolePermission::getRoleId, role.getId()));
        if (existingCount > 0) {
            return;
        }
        for (String code : codes) {
            SysPermission permission = permissions.get(code);
            if (permission == null) {
                log.warn("skip seeding unknown permission code {} for role {}", code, role.getRoleCode());
                continue;
            }
            sysRolePermissionMapper.insert(new SysRolePermission(role.getId(), permission.getId()));
        }
    }

    private void seedAdminUser(Map<String, SysRole> roles) {
        SysUser admin = sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class).eq(SysUser::getUsername, ADMIN_USERNAME));
        if (admin == null) {
            admin = new SysUser(ADMIN_USERNAME, passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD), "系统管理员", null, null);
            sysUserMapper.insert(admin);
            log.info("seeded default admin account (username={}, password={})", ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD);
        }

        SysRole adminRole = roles.get("ADMIN");
        long assigned = sysUserRoleMapper.selectCount(Wrappers.lambdaQuery(SysUserRole.class).eq(SysUserRole::getUserId, admin.getId()));
        if (assigned == 0) {
            sysUserRoleMapper.insert(new SysUserRole(admin.getId(), adminRole.getId()));
        }
    }
}
