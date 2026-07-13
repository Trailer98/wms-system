-- V7__ai_knowledge_dicts.sql
-- Display dictionaries for the P0 AI knowledge base enums. These enums are code-level (the backend
-- validates against fixed values); the dict only carries display label/sort/tag colour, mirroring V3.
-- is_system = 1 so the dict admin UI won't physically delete them. Idempotent via ON DUPLICATE KEY UPDATE.

INSERT INTO sys_dict_type (id, dict_code, dict_name, status, remark, sort_order, created_by, created_at, updated_by, updated_at)
VALUES
  (UUID_SHORT(), 'ai_knowledge_module',         'AI知识-业务模块', 'ENABLED', 'AI 知识库文档所属业务模块', 10, 'system', NOW(), NULL, NOW()),
  (UUID_SHORT(), 'ai_knowledge_source_type',    'AI知识-来源类型', 'ENABLED', 'AI 知识库文档来源类型', 20, 'system', NOW(), NULL, NOW()),
  (UUID_SHORT(), 'ai_knowledge_status',         'AI知识-文档状态', 'ENABLED', 'AI 知识库文档状态', 30, 'system', NOW(), NULL, NOW()),
  (UUID_SHORT(), 'ai_knowledge_content_format', 'AI知识-内容格式', 'ENABLED', 'AI 知识库正文格式', 40, 'system', NOW(), NULL, NOW()),
  (UUID_SHORT(), 'ai_knowledge_chunk_strategy', 'AI知识-分片策略', 'ENABLED', 'AI 知识库分片策略', 50, 'system', NOW(), NULL, NOW()),
  (UUID_SHORT(), 'ai_knowledge_vector_status',  'AI知识-向量状态', 'ENABLED', 'AI 知识库分片向量化状态', 60, 'system', NOW(), NULL, NOW())
ON DUPLICATE KEY UPDATE
  dict_name = VALUES(dict_name),
  remark = VALUES(remark),
  sort_order = VALUES(sort_order),
  updated_at = NOW();

INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, item_label_en, sort_order, status, tag_type, css_class, is_system, remark, created_by, created_at, updated_by, updated_at, deleted)
VALUES
  -- module
  (UUID_SHORT(), 'ai_knowledge_module', 'INVENTORY',    '库存管理', NULL, 10,  'ENABLED', 'primary', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'INBOUND',      '入库管理', NULL, 20,  'ENABLED', 'success', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'OUTBOUND',     '出库管理', NULL, 30,  'ENABLED', 'warning', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'STOCK_ADJUST', '库存调整', NULL, 40,  'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'STOCK_COUNT',  '库存盘点', NULL, 50,  'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'EXCEPTION',    '异常管理', NULL, 60,  'ENABLED', 'danger',  NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'FINANCE',      '财务管理', NULL, 70,  'ENABLED', '',        NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'RBAC',         '权限管理', NULL, 80,  'ENABLED', '',        NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'OPERATION_LOG','操作日志', NULL, 90,  'ENABLED', '',        NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'TOOL_CALLING', '工具调用', NULL, 100, 'ENABLED', '',        NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_module', 'SYSTEM',       '系统通用', NULL, 110, 'ENABLED', '',        NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  -- source_type
  (UUID_SHORT(), 'ai_knowledge_source_type', 'RULE',            '业务规则', NULL, 10, 'ENABLED', 'primary', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_source_type', 'SOP',             '操作手册', NULL, 20, 'ENABLED', 'success', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_source_type', 'FIELD_DICT',      '字段字典', NULL, 30, 'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_source_type', 'EXCEPTION_GUIDE', '异常指引', NULL, 40, 'ENABLED', 'danger',  NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_source_type', 'TOOL_GUIDE',      '工具指引', NULL, 50, 'ENABLED', 'warning', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_source_type', 'ARCHITECTURE',    '架构说明', NULL, 60, 'ENABLED', '',        NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_source_type', 'FAQ',             '常见问题', NULL, 70, 'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  -- document status
  (UUID_SHORT(), 'ai_knowledge_status', 'DRAFT',    '草稿', NULL, 10, 'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_status', 'ENABLED',  '启用', NULL, 20, 'ENABLED', 'success', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_status', 'DISABLED', '停用', NULL, 30, 'ENABLED', 'danger',  NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  -- content format
  (UUID_SHORT(), 'ai_knowledge_content_format', 'MARKDOWN', 'Markdown', NULL, 10, 'ENABLED', 'primary', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_content_format', 'TEXT',     '纯文本',   NULL, 20, 'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  -- chunk strategy
  (UUID_SHORT(), 'ai_knowledge_chunk_strategy', 'SEMANTIC_MARKDOWN', '语义分片(Markdown)', NULL, 10, 'ENABLED', 'primary', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_chunk_strategy', 'FIXED_SIZE',        '固定长度',           NULL, 20, 'ENABLED', 'warning', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_chunk_strategy', 'NONE',              '不分片',             NULL, 30, 'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  -- vector status
  (UUID_SHORT(), 'ai_knowledge_vector_status', 'PENDING', '待向量化', NULL, 10, 'ENABLED', 'info',    NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_vector_status', 'INDEXED', '已索引',   NULL, 20, 'ENABLED', 'success', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_vector_status', 'FAILED',  '失败',     NULL, 30, 'ENABLED', 'danger',  NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'ai_knowledge_vector_status', 'STALE',   '已过期',   NULL, 40, 'ENABLED', 'warning', NULL, 1, NULL, 'system', NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  item_label = VALUES(item_label),
  sort_order = VALUES(sort_order),
  tag_type = VALUES(tag_type),
  updated_at = NOW();
