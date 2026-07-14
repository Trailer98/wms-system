-- V9__add_developer_role.sql
-- Adds a DEVELOPER role for developers/testers/implementers/technical debugging staff and grants it
-- every existing permission, so RAG answers can switch to a technical mode for this role (see
-- AiRagAskService.resolveAudienceMode) without any other page/button being blocked for them.
--
-- Idempotent, same pattern as V2/V6/V8: role upserts on the unique role_code; grants resolve role by
-- code and rely on the existing UNIQUE (role_id, permission_id) constraint on sys_role_permission
-- (see V1) via ON DUPLICATE KEY UPDATE — no schema change needed, no risk of duplicate rows.
--
-- IMPORTANT — going forward: any migration that adds a new sys_permission row must grant it to BOTH
-- ADMIN and DEVELOPER (see V2/V6/V8 for the ADMIN grant pattern this mirrors), so DEVELOPER never
-- silently falls behind ADMIN's permission set. See also docs/database-migration.md.

INSERT INTO sys_role (id, role_code, role_name, status, remark, create_time, update_time, deleted)
VALUES
  (UUID_SHORT(), 'DEVELOPER', '开发者', 'ENABLED',
   '面向开发、测试、实施和技术调试人员，拥有系统全部页面和按钮权限，RAG 回答默认使用技术人员模式。',
   NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  remark = VALUES(remark),
  status = VALUES(status),
  update_time = NOW();

-- DEVELOPER: every permission that exists today (mirrors the ADMIN grant in V2).
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT UUID_SHORT(), r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.role_code = 'DEVELOPER'
ON DUPLICATE KEY UPDATE role_id = role_id;
