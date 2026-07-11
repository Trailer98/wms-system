-- V3__dict_base_data.sql
-- Stock-movement display dictionaries previously seeded by DictDataInitializer.java (now disabled).
-- Display data only (label/sort/tag colour); the OperationType/MovementType enums remain the source of
-- truth for business logic. is_system = 1 marks these as code-origin (the dict admin UI refuses to
-- physically delete them). Idempotent via ON DUPLICATE KEY UPDATE on the natural unique keys.

-- Dict types (dict_code UNIQUE)
INSERT INTO sys_dict_type (id, dict_code, dict_name, status, remark, sort_order, created_by, created_at, updated_by, updated_at)
VALUES
  (UUID_SHORT(), 'stock_movement_operation_type', '库存流水操作类型', 'ENABLED', '对应 Java 枚举 OperationType，取值由后端业务逻辑决定，本字典只负责展示文案/排序/标签样式', 10, 'system', NOW(), NULL, NOW()),
  (UUID_SHORT(), 'stock_movement_biz_type', '库存流水业务类型', 'ENABLED', '对应 Java 枚举 MovementType（前端历史上称之为“业务类型”），取值由后端业务逻辑决定', 20, 'system', NOW(), NULL, NOW())
ON DUPLICATE KEY UPDATE
  dict_name = VALUES(dict_name),
  remark = VALUES(remark),
  sort_order = VALUES(sort_order),
  updated_at = NOW();

-- Dict items ((dict_code, item_value) UNIQUE). tag_type is '' (empty) for ADJUSTMENT/COUNT, matching the seeder.
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, item_label_en, sort_order, status, tag_type, css_class, is_system, remark, created_by, created_at, updated_by, updated_at, deleted)
VALUES
  (UUID_SHORT(), 'stock_movement_operation_type', 'INBOUND_RECEIVE', '入库收货', NULL, 10, 'ENABLED', 'success', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'OUTBOUND_LOCK', '出库锁库', NULL, 20, 'ENABLED', 'warning', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'OUTBOUND_CANCEL_UNLOCK', '出库取消解锁', NULL, 30, 'ENABLED', 'info', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'OUTBOUND_SHIP', '出库发货', NULL, 40, 'ENABLED', 'success', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'STOCK_ADJUST_INCREASE', '库存调整增加', NULL, 50, 'ENABLED', 'success', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'STOCK_ADJUST_DECREASE', '库存调整减少', NULL, 60, 'ENABLED', 'danger', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'STOCK_COUNT_PROFIT', '库存盘点盘盈', NULL, 70, 'ENABLED', 'success', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'STOCK_COUNT_LOSS', '库存盘点盘亏', NULL, 80, 'ENABLED', 'danger', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'STOCK_FREEZE', '库存冻结', NULL, 90, 'ENABLED', 'warning', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'STOCK_UNFREEZE', '库存解冻', NULL, 100, 'ENABLED', 'info', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'TRANSFER_OUT', '移库转出', NULL, 110, 'ENABLED', 'warning', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'TRANSFER_IN', '移库转入', NULL, 120, 'ENABLED', 'success', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'TRANSFER_TO_EXCEPTION_OUT', '转异常区-转出', NULL, 130, 'ENABLED', 'warning', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'TRANSFER_TO_EXCEPTION_IN', '转异常区-转入', NULL, 140, 'ENABLED', 'danger', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'RESTORE_FROM_EXCEPTION_OUT', '异常恢复-转出', NULL, 150, 'ENABLED', 'warning', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'RESTORE_FROM_EXCEPTION_IN', '异常恢复-转入', NULL, 160, 'ENABLED', 'success', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_operation_type', 'UNKNOWN', '未知操作', NULL, 999, 'ENABLED', 'info', NULL, 1, 'seeded from stock_movement_operation_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_biz_type', 'INBOUND', '入库', NULL, 10, 'ENABLED', 'success', NULL, 1, 'seeded from stock_movement_biz_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_biz_type', 'OUTBOUND', '出库', NULL, 20, 'ENABLED', 'warning', NULL, 1, 'seeded from stock_movement_biz_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_biz_type', 'ADJUSTMENT', '库存调整', NULL, 30, 'ENABLED', '', NULL, 1, 'seeded from stock_movement_biz_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_biz_type', 'LOCK', '锁库', NULL, 40, 'ENABLED', 'warning', NULL, 1, 'seeded from stock_movement_biz_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_biz_type', 'UNLOCK', '解锁', NULL, 50, 'ENABLED', 'info', NULL, 1, 'seeded from stock_movement_biz_type enum', 'system', NOW(), NULL, NOW(), 0),
  (UUID_SHORT(), 'stock_movement_biz_type', 'COUNT', '库存盘点', NULL, 60, 'ENABLED', '', NULL, 1, 'seeded from stock_movement_biz_type enum', 'system', NOW(), NULL, NOW(), 0)
ON DUPLICATE KEY UPDATE
  item_label = VALUES(item_label),
  sort_order = VALUES(sort_order),
  tag_type = VALUES(tag_type),
  remark = VALUES(remark),
  updated_at = NOW();
