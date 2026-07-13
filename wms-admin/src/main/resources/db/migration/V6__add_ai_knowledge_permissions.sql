-- V6__add_ai_knowledge_permissions.sql
-- Permission points for the P0 AI knowledge base, plus role grants. Idempotent (same pattern as V2):
-- permissions upsert on the unique permission_code; grants resolve role/permission BY CODE so they are
-- safe to re-run and independent of id origin. New permissions are NOT covered by V2's one-time ADMIN
-- grant, so ADMIN is (re)granted the new codes here explicitly.

INSERT INTO sys_permission (id, permission_code, permission_name, permission_type, parent_id, path, method, sort, status, remark, create_time, update_time)
VALUES
  (UUID_SHORT(), 'ai-knowledge:view',      'AI知识库查看',   'MENU',   NULL, NULL, NULL, 180, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'ai-knowledge:create',    'AI知识新增',     'BUTTON', NULL, NULL, NULL, 181, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'ai-knowledge:update',    'AI知识编辑',     'BUTTON', NULL, NULL, NULL, 182, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'ai-knowledge:disable',   'AI知识启停',     'BUTTON', NULL, NULL, NULL, 183, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'ai-knowledge:vectorize', 'AI知识重新向量化', 'BUTTON', NULL, NULL, NULL, 184, 'ENABLED', NULL, NOW(), NOW()),
  (UUID_SHORT(), 'ai-knowledge:search',    'AI知识检索测试', 'BUTTON', NULL, NULL, NULL, 185, 'ENABLED', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  sort = VALUES(sort),
  update_time = NOW();

-- ADMIN: all AI knowledge permissions
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'ADMIN'
  AND p.permission_code LIKE 'ai-knowledge:%'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- WAREHOUSE_MANAGER: view + search (read + retrieval test, no authoring)
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'WAREHOUSE_MANAGER'
  AND p.permission_code IN ('ai-knowledge:view', 'ai-knowledge:search')
ON DUPLICATE KEY UPDATE role_id = role_id;

-- WAREHOUSE_OPERATOR: search only
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'WAREHOUSE_OPERATOR'
  AND p.permission_code = 'ai-knowledge:search'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- INVENTORY_VIEWER: search only (read-only retrieval test, consistent with its view-only style)
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'INVENTORY_VIEWER'
  AND p.permission_code = 'ai-knowledge:search'
ON DUPLICATE KEY UPDATE role_id = role_id;
