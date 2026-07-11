-- V2__system_base_data.sql
-- RBAC baseline previously seeded by DataInitializer.java (now disabled). Idempotent by design so it
-- runs safely on both a fresh DB and an already-populated dev/test DB that Flyway baselines at V1:
--   * master rows (permissions/roles/user) upsert on their natural unique keys via ON DUPLICATE KEY UPDATE;
--   * grants resolve role/permission by CODE (not by id) so they work regardless of whether ids are the
--     fixed values here or the snowflake ids an earlier DataInitializer run produced;
--   * ids use UUID_SHORT() (only consumed when a row is actually inserted; ignored on duplicate).
-- The admin row is inserted only if absent (ON DUPLICATE ... username=username is a no-op), so an
-- operator's changed password is never overwritten. No business data is inserted here.

-- ---------------------------------------------------------------------------------------------------
-- Permission points (permission_code is UNIQUE)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO sys_permission (id, permission_code, permission_name, permission_type, parent_id, path, method, sort, status, remark, create_time, update_time)
VALUES
  (UUID_SHORT(), 'warehouse:view', '仓库查看', 'MENU', NULL, NULL, NULL, 10, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'warehouse:create', '新增仓库', 'BUTTON', NULL, NULL, NULL, 11, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'warehouse:update', '编辑仓库', 'BUTTON', NULL, NULL, NULL, 12, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'warehouse:disable', '停用仓库', 'BUTTON', NULL, NULL, NULL, 13, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'area:view', '库区查看', 'MENU', NULL, NULL, NULL, 20, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'area:create', '新增库区', 'BUTTON', NULL, NULL, NULL, 21, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'area:update', '编辑库区', 'BUTTON', NULL, NULL, NULL, 22, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'area:disable', '停用库区', 'BUTTON', NULL, NULL, NULL, 23, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'location:view', '库位查看', 'MENU', NULL, NULL, NULL, 30, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'location:create', '新增库位', 'BUTTON', NULL, NULL, NULL, 31, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'location:update', '编辑库位', 'BUTTON', NULL, NULL, NULL, 32, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'location:disable', '停用库位', 'BUTTON', NULL, NULL, NULL, 33, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sku:view', 'SKU查看', 'MENU', NULL, NULL, NULL, 40, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sku:create', '新增SKU', 'BUTTON', NULL, NULL, NULL, 41, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sku:update', '编辑SKU', 'BUTTON', NULL, NULL, NULL, 42, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sku:disable', '停用SKU', 'BUTTON', NULL, NULL, NULL, 43, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inbound:view', '入库查看', 'MENU', NULL, NULL, NULL, 50, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inbound:create', '创建入库单', 'BUTTON', NULL, NULL, NULL, 51, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inbound:update', '编辑入库单', 'BUTTON', NULL, NULL, NULL, 52, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inbound:complete', '完成入库', 'BUTTON', NULL, NULL, NULL, 53, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inbound:cancel', '取消/删除入库单', 'BUTTON', NULL, NULL, NULL, 54, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'outbound:view', '出库查看', 'MENU', NULL, NULL, NULL, 60, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'outbound:create', '创建出库单', 'BUTTON', NULL, NULL, NULL, 61, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'outbound:lock', '出库锁库', 'BUTTON', NULL, NULL, NULL, 62, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'outbound:confirm', '确认出库', 'BUTTON', NULL, NULL, NULL, 63, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'outbound:cancel', '取消出库', 'BUTTON', NULL, NULL, NULL, 64, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'outbound:allocation:view', '出库分配明细查看', 'BUTTON', NULL, NULL, NULL, 65, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inventory:view', '库存查看', 'MENU', NULL, NULL, NULL, 70, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inventory:transaction:view', '库存流水查看', 'MENU', NULL, NULL, NULL, 71, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inventory:adjust', '库存调整', 'BUTTON', NULL, NULL, NULL, 72, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'inventory:count', '库存盘点', 'BUTTON', NULL, NULL, NULL, 73, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'exception:view', '异常查看', 'MENU', NULL, NULL, NULL, 80, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'exception:handle', '异常处理', 'BUTTON', NULL, NULL, NULL, 81, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'user:view', '用户查看', 'MENU', NULL, NULL, NULL, 90, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'user:create', '新增用户', 'BUTTON', NULL, NULL, NULL, 91, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'user:update', '编辑用户', 'BUTTON', NULL, NULL, NULL, 92, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'user:disable', '停用用户', 'BUTTON', NULL, NULL, NULL, 93, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'role:view', '角色查看', 'MENU', NULL, NULL, NULL, 100, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'role:create', '新增角色', 'BUTTON', NULL, NULL, NULL, 101, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'role:update', '编辑角色', 'BUTTON', NULL, NULL, NULL, 102, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'role:assign', '分配角色权限', 'BUTTON', NULL, NULL, NULL, 103, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'permission:view', '权限查看', 'MENU', NULL, NULL, NULL, 110, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'permission:assign', '权限分配', 'BUTTON', NULL, NULL, NULL, 111, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-adjust:view', '库存调整查看', 'MENU', NULL, NULL, NULL, 120, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-adjust:create', '创建库存调整单', 'BUTTON', NULL, NULL, NULL, 121, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-adjust:update', '编辑库存调整单', 'BUTTON', NULL, NULL, NULL, 122, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-adjust:submit', '提交库存调整单', 'BUTTON', NULL, NULL, NULL, 123, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-adjust:confirm', '确认库存调整单', 'BUTTON', NULL, NULL, NULL, 124, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-adjust:cancel', '取消库存调整单', 'BUTTON', NULL, NULL, NULL, 125, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-count:view', '库存盘点查看', 'MENU', NULL, NULL, NULL, 130, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-count:create', '创建库存盘点任务', 'BUTTON', NULL, NULL, NULL, 131, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-count:start', '开始库存盘点', 'BUTTON', NULL, NULL, NULL, 132, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-count:record', '录入盘点实盘数量', 'BUTTON', NULL, NULL, NULL, 133, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-count:complete', '完成库存盘点', 'BUTTON', NULL, NULL, NULL, 134, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'stock-count:cancel', '取消库存盘点', 'BUTTON', NULL, NULL, NULL, 135, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'customer:view', '客户查看', 'MENU', NULL, NULL, NULL, 140, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'customer:create', '新增客户', 'BUTTON', NULL, NULL, NULL, 141, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'customer:update', '编辑客户', 'BUTTON', NULL, NULL, NULL, 142, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'customer:disable', '停用客户', 'BUTTON', NULL, NULL, NULL, 143, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'supplier:view', '供应商查看', 'MENU', NULL, NULL, NULL, 150, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'supplier:create', '新增供应商', 'BUTTON', NULL, NULL, NULL, 151, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'supplier:update', '编辑供应商', 'BUTTON', NULL, NULL, NULL, 152, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'supplier:disable', '停用供应商', 'BUTTON', NULL, NULL, NULL, 153, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'operation-log:view', '操作日志查看', 'MENU', NULL, NULL, NULL, 160, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sys-dict:view', '数据字典查看', 'MENU', NULL, NULL, NULL, 170, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sys-dict:create', '新增字典', 'BUTTON', NULL, NULL, NULL, 171, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sys-dict:update', '编辑字典', 'BUTTON', NULL, NULL, NULL, 172, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sys-dict:disable', '启停字典', 'BUTTON', NULL, NULL, NULL, 173, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'sys-dict:delete', '删除字典项', 'BUTTON', NULL, NULL, NULL, 174, 'ENABLED', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  sort = VALUES(sort),
  update_time = NOW();

-- ---------------------------------------------------------------------------------------------------
-- Roles (role_code is UNIQUE)
-- ---------------------------------------------------------------------------------------------------
INSERT INTO sys_role (id, role_code, role_name, status, remark, create_time, update_time, deleted)
VALUES
  (UUID_SHORT(), 'ADMIN', '系统管理员', 'ENABLED', '拥有全部权限', NOW(), NOW(), 0),
  (UUID_SHORT(), 'WAREHOUSE_MANAGER', '仓库主管', 'ENABLED', '拥有全部仓储业务权限，不含用户与角色管理', NOW(), NOW(), 0),
  (UUID_SHORT(), 'WAREHOUSE_OPERATOR', '仓管员', 'ENABLED', '负责日常入库、出库操作', NOW(), NOW(), 0),
  (UUID_SHORT(), 'INVENTORY_VIEWER', '库存查看员', 'ENABLED', '只读查看库存、流水与单据', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  remark = VALUES(remark),
  update_time = NOW();

-- ---------------------------------------------------------------------------------------------------
-- Default admin account (username is UNIQUE). Inserted only if absent so a changed password survives.
-- password_hash is a Hutool-BCrypt hash of "admin123" (same encoder the app uses at login).
-- ---------------------------------------------------------------------------------------------------
INSERT INTO sys_user (id, username, password_hash, real_name, phone, email, status, last_login_time, create_time, update_time, deleted)
VALUES
  (UUID_SHORT(), 'admin', '$2a$10$X6abw9Phw05P7lPxpEjpGOdsXnkKEU8NT9xSwc/Ctw.PTmi3xPAc.', '系统管理员', NULL, NULL, 'ENABLED', NULL, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE username = username;

-- admin -> ADMIN role
INSERT INTO sys_user_role (id, user_id, role_id)
SELECT UUID_SHORT(), u.id, r.id
FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE user_id = user_id;

-- ---------------------------------------------------------------------------------------------------
-- Role -> permission grants (resolved by code; mirrors DataInitializer's per-role logic exactly)
-- ---------------------------------------------------------------------------------------------------

-- ADMIN: every permission
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- WAREHOUSE_MANAGER: everything except user:/role:/permission: and sys-dict management (keeps sys-dict:view)
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'WAREHOUSE_MANAGER'
  AND p.permission_code NOT LIKE 'user:%'
  AND p.permission_code NOT LIKE 'role:%'
  AND p.permission_code NOT LIKE 'permission:%'
  AND (p.permission_code NOT LIKE 'sys-dict:%' OR p.permission_code = 'sys-dict:view')
ON DUPLICATE KEY UPDATE role_id = role_id;

-- WAREHOUSE_OPERATOR: day-to-day operational permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'WAREHOUSE_OPERATOR'
  AND p.permission_code IN (
    'warehouse:view', 'area:view', 'location:view', 'sku:view',
    'inbound:view', 'inbound:create', 'inbound:update', 'inbound:complete', 'inbound:cancel',
    'outbound:view', 'outbound:create', 'outbound:lock', 'outbound:confirm', 'outbound:cancel', 'outbound:allocation:view',
    'inventory:view', 'inventory:transaction:view',
    'exception:view',
    'stock-adjust:view', 'stock-adjust:create', 'stock-adjust:update', 'stock-adjust:submit',
    'stock-count:view', 'stock-count:create', 'stock-count:start', 'stock-count:record',
    'customer:view', 'supplier:view',
    'sys-dict:view'
  )
ON DUPLICATE KEY UPDATE role_id = role_id;

-- INVENTORY_VIEWER: read-only view of inventory, movements and documents
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'INVENTORY_VIEWER'
  AND p.permission_code IN (
    'warehouse:view', 'area:view', 'location:view', 'sku:view',
    'inbound:view', 'outbound:view',
    'inventory:view', 'inventory:transaction:view',
    'exception:view',
    'stock-adjust:view', 'stock-count:view',
    'customer:view', 'supplier:view',
    'sys-dict:view'
  )
ON DUPLICATE KEY UPDATE role_id = role_id;
