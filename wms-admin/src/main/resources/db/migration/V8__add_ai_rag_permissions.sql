-- V8__add_ai_rag_permissions.sql
-- Permission point for the P0 RAG ask endpoint (POST /ai/rag/ask), plus role grants. Same idempotent
-- pattern as V6: permission upserts on the unique permission_code; grants resolve role/permission by
-- CODE so they're safe to re-run.
--
-- INVENTORY_VIEWER is intentionally NOT granted ai-rag:ask: unlike ai-knowledge:search (a read-only
-- retrieval-test action), asking the RAG endpoint invokes the paid DeepSeek chat model on every call,
-- so it is scoped to roles that actively work orders (ADMIN/WAREHOUSE_MANAGER/WAREHOUSE_OPERATOR).

INSERT INTO sys_permission (id, permission_code, permission_name, permission_type, parent_id, path, method, sort, status, remark, create_time, update_time)
VALUES
  (UUID_SHORT(), 'ai-rag:ask', 'AI RAG 问答', 'BUTTON', NULL, NULL, NULL, 186, 'ENABLED', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  sort = VALUES(sort),
  update_time = NOW();

-- ADMIN
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'ADMIN' AND p.permission_code = 'ai-rag:ask'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- WAREHOUSE_MANAGER
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'WAREHOUSE_MANAGER' AND p.permission_code = 'ai-rag:ask'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- WAREHOUSE_OPERATOR
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'WAREHOUSE_OPERATOR' AND p.permission_code = 'ai-rag:ask'
ON DUPLICATE KEY UPDATE role_id = role_id;
