create table if not exists warehouses (
    id bigint not null,
    code varchar(64) not null unique,
    name varchar(128) not null,
    address varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists skus (
    id bigint not null,
    code varchar(64) not null unique,
    name varchar(128) not null,
    unit varchar(32) not null,
    category varchar(64),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists customers (
    id bigint not null,
    code varchar(64) not null unique,
    name varchar(128) not null,
    contact_name varchar(64),
    contact_phone varchar(32),
    address varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists suppliers (
    id bigint not null,
    code varchar(64) not null unique,
    name varchar(128) not null,
    contact_name varchar(64),
    contact_phone varchar(32),
    address varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists inventory (
    id bigint not null,
    warehouse_id bigint not null,
    sku_id bigint not null,
    quantity integer not null,
    reserved_quantity integer not null,
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    constraint uk_inventory_warehouse_sku unique (warehouse_id, sku_id)
);

create table if not exists inbound_orders (
    id bigint not null,
    order_no varchar(64) not null unique,
    status varchar(32) not null,
    warehouse_id bigint not null,
    supplier_id bigint,
    received_at timestamp,
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists inbound_order_items (
    id bigint not null,
    order_id bigint not null,
    sku_id bigint not null,
    quantity integer not null,
    enabled tinyint(1) not null,
    primary key (id)
);

create table if not exists outbound_orders (
    id bigint not null,
    order_no varchar(64) not null unique,
    status varchar(32) not null,
    warehouse_id bigint not null,
    customer_id bigint,
    shipped_at timestamp,
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists outbound_order_items (
    id bigint not null,
    order_id bigint not null,
    sku_id bigint not null,
    quantity integer not null,
    enabled tinyint(1) not null,
    primary key (id)
);

create table if not exists stock_movements (
    id bigint not null,
    type varchar(32) not null,
    warehouse_id bigint not null,
    sku_id bigint not null,
    quantity_change integer not null,
    business_no varchar(64) not null,
    remark varchar(255),
    occurred_at timestamp not null,
    enabled tinyint(1) not null,
    primary key (id)
);

create table if not exists sys_operation_log (
    id bigint not null,
    operator varchar(64) not null,
    operation_type varchar(64) not null,
    biz_no varchar(128),
    biz_type varchar(64),
    biz_id bigint,
    content varchar(255),
    ip varchar(64),
    create_time timestamp not null,
    primary key (id)
);

create table if not exists warehouse_areas (
    id bigint not null,
    warehouse_id bigint not null,
    area_code varchar(64) not null,
    area_name varchar(128) not null,
    area_type varchar(32) not null,
    status varchar(32) not null,
    pick_priority integer not null default 0,
    remark varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    constraint uk_warehouse_area_code unique (warehouse_id, area_code)
);

create table if not exists warehouse_locations (
    id bigint not null,
    warehouse_id bigint not null,
    area_id bigint not null,
    location_code varchar(64) not null,
    location_name varchar(128),
    location_type varchar(32) not null,
    status varchar(32) not null,
    capacity_qty integer not null default 0,
    used_qty integer not null default 0,
    allow_mixed_sku tinyint(1) not null default 1,
    pick_priority integer not null default 0,
    remark varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    constraint uk_warehouse_location_code unique (warehouse_id, location_code)
);

create table if not exists outbound_stock_locks (
    id bigint not null,
    outbound_order_id bigint not null,
    outbound_order_item_id bigint not null,
    sku_id bigint not null,
    warehouse_id bigint not null,
    area_id bigint not null,
    location_id bigint not null,
    lock_qty integer not null,
    shipped_qty integer not null default 0,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    index idx_outbound_stock_lock_order (outbound_order_id, outbound_order_item_id)
);

create table if not exists wms_exception_events (
    id bigint not null,
    exception_type varchar(32) not null,
    biz_no varchar(64),
    sku_id bigint,
    warehouse_id bigint,
    area_id bigint,
    location_id bigint,
    message varchar(500),
    status varchar(32) not null default 'OPEN',
    handler_id bigint,
    handled_time timestamp null,
    create_time timestamp not null,
    primary key (id)
);

-- MySQL has no ADD COLUMN/INDEX IF NOT EXISTS clause, and schema.sql is re-executed on every
-- application startup (spring.sql.init.mode=always), so the ALTERs below are guarded through
-- information_schema + dynamic SQL to stay idempotent across restarts.

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory' AND column_name = 'area_id') = 0,
    'ALTER TABLE inventory ADD COLUMN area_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory' AND column_name = 'location_id') = 0,
    'ALTER TABLE inventory ADD COLUMN location_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory' AND column_name = 'frozen_quantity') = 0,
    'ALTER TABLE inventory ADD COLUMN frozen_quantity INTEGER NOT NULL DEFAULT 0', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inventory' AND column_name = 'inventory_status') = 0,
    'ALTER TABLE inventory ADD COLUMN inventory_status VARCHAR(32) NOT NULL DEFAULT ''NORMAL''', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'uk_inventory_warehouse_sku') > 0,
    'ALTER TABLE inventory DROP INDEX uk_inventory_warehouse_sku', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory' AND index_name = 'uk_inventory_dimension') = 0,
    'ALTER TABLE inventory ADD UNIQUE INDEX uk_inventory_dimension (warehouse_id, sku_id, area_id, location_id)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'area_id') = 0,
    'ALTER TABLE stock_movements ADD COLUMN area_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'location_id') = 0,
    'ALTER TABLE stock_movements ADD COLUMN location_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'before_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN before_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'after_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN after_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'before_reserved_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN before_reserved_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'after_reserved_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN after_reserved_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'operator') = 0,
    'ALTER TABLE stock_movements ADD COLUMN operator VARCHAR(64) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'before_frozen_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN before_frozen_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'after_frozen_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN after_frozen_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'before_available_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN before_available_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'after_available_quantity') = 0,
    'ALTER TABLE stock_movements ADD COLUMN after_available_quantity INTEGER NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inbound_order_items' AND column_name = 'area_id') = 0,
    'ALTER TABLE inbound_order_items ADD COLUMN area_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inbound_order_items' AND column_name = 'location_id') = 0,
    'ALTER TABLE inbound_order_items ADD COLUMN location_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- inventory transaction operation_type (orthogonal to biz_type: "what changed" vs "which business triggered it")
SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND column_name = 'operation_type') = 0,
    'ALTER TABLE stock_movements ADD COLUMN operation_type VARCHAR(32) NOT NULL DEFAULT ''UNKNOWN''', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ERP-reserved fields on inbound/outbound orders (no push flow implemented, MANUAL is the only value used in V1)
SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inbound_orders' AND column_name = 'source_type') = 0,
    'ALTER TABLE inbound_orders ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT ''MANUAL''', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inbound_orders' AND column_name = 'source_order_no') = 0,
    'ALTER TABLE inbound_orders ADD COLUMN source_order_no VARCHAR(64) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'inbound_orders' AND column_name = 'external_order_no') = 0,
    'ALTER TABLE inbound_orders ADD COLUMN external_order_no VARCHAR(64) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'outbound_orders' AND column_name = 'source_type') = 0,
    'ALTER TABLE outbound_orders ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT ''MANUAL''', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'outbound_orders' AND column_name = 'source_order_no') = 0,
    'ALTER TABLE outbound_orders ADD COLUMN source_order_no VARCHAR(64) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'outbound_orders' AND column_name = 'external_order_no') = 0,
    'ALTER TABLE outbound_orders ADD COLUMN external_order_no VARCHAR(64) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- minimal RBAC: users, roles, permissions and their relations
create table if not exists sys_user (
    id bigint not null,
    username varchar(64) not null,
    password_hash varchar(100) not null,
    real_name varchar(64),
    phone varchar(32),
    email varchar(128),
    status varchar(32) not null default 'ENABLED',
    last_login_time timestamp null,
    create_time timestamp not null,
    update_time timestamp not null,
    deleted tinyint(1) not null default 0,
    primary key (id),
    constraint uk_sys_user_username unique (username)
);

create table if not exists sys_role (
    id bigint not null,
    role_code varchar(64) not null,
    role_name varchar(64) not null,
    status varchar(32) not null default 'ENABLED',
    remark varchar(255),
    create_time timestamp not null,
    update_time timestamp not null,
    deleted tinyint(1) not null default 0,
    primary key (id),
    constraint uk_sys_role_code unique (role_code)
);

create table if not exists sys_permission (
    id bigint not null,
    permission_code varchar(100) not null,
    permission_name varchar(100) not null,
    permission_type varchar(32) not null,
    parent_id bigint,
    path varchar(255),
    method varchar(16),
    sort integer not null default 0,
    status varchar(32) not null default 'ENABLED',
    remark varchar(255),
    create_time timestamp not null,
    update_time timestamp not null,
    primary key (id),
    constraint uk_sys_permission_code unique (permission_code)
);

create table if not exists sys_user_role (
    id bigint not null,
    user_id bigint not null,
    role_id bigint not null,
    primary key (id),
    constraint uk_sys_user_role unique (user_id, role_id)
);

create table if not exists sys_role_permission (
    id bigint not null,
    role_id bigint not null,
    permission_id bigint not null,
    primary key (id),
    constraint uk_sys_role_permission unique (role_id, permission_id)
);

-- attach identity + outcome info to the existing operation log table
SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'user_id') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN user_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'module') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN module VARCHAR(64) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'request_uri') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN request_uri VARCHAR(255) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'request_method') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN request_method VARCHAR(16) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'success') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN success TINYINT(1) NOT NULL DEFAULT 1', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'error_message') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN error_message VARCHAR(500) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'biz_type') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN biz_type VARCHAR(64) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND column_name = 'biz_id') = 0,
    'ALTER TABLE sys_operation_log ADD COLUMN biz_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND index_name = 'idx_operation_log_biz_type_no') = 0,
    'ALTER TABLE sys_operation_log ADD INDEX idx_operation_log_biz_type_no (biz_type, biz_no)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_operation_log' AND index_name = 'idx_operation_log_biz_id') = 0,
    'ALTER TABLE sys_operation_log ADD INDEX idx_operation_log_biz_id (biz_id)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- operation_type semantic rework: rename technical values to business-action values on existing rows.
-- Plain UPDATEs are inherently idempotent (after the first run no row still holds the old value).
UPDATE stock_movements SET operation_type = 'INBOUND_RECEIVE' WHERE operation_type = 'ON_HAND_INCREASE' AND type = 'INBOUND';
UPDATE stock_movements SET operation_type = 'OUTBOUND_SHIP' WHERE operation_type = 'ON_HAND_DECREASE' AND type = 'OUTBOUND';
UPDATE stock_movements SET operation_type = 'OUTBOUND_LOCK' WHERE operation_type = 'STOCK_LOCK';
UPDATE stock_movements SET operation_type = 'OUTBOUND_CANCEL_UNLOCK' WHERE operation_type = 'STOCK_UNLOCK';
UPDATE stock_movements SET operation_type = 'UNKNOWN' WHERE operation_type NOT IN (
    'INBOUND_RECEIVE', 'OUTBOUND_LOCK', 'OUTBOUND_CANCEL_UNLOCK', 'OUTBOUND_SHIP',
    'STOCK_ADJUST_INCREASE', 'STOCK_ADJUST_DECREASE', 'STOCK_COUNT_PROFIT', 'STOCK_COUNT_LOSS',
    'STOCK_FREEZE', 'STOCK_UNFREEZE', 'TRANSFER_OUT', 'TRANSFER_IN',
    'TRANSFER_TO_EXCEPTION_OUT', 'TRANSFER_TO_EXCEPTION_IN', 'RESTORE_FROM_EXCEPTION_OUT', 'RESTORE_FROM_EXCEPTION_IN',
    'UNKNOWN'
);

-- stock_movements never had any indexes despite being filtered/sorted on all of these; add them now.
SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND index_name = 'idx_stock_movements_operation_type') = 0,
    'ALTER TABLE stock_movements ADD INDEX idx_stock_movements_operation_type (operation_type)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND index_name = 'idx_stock_movements_type_business_no') = 0,
    'ALTER TABLE stock_movements ADD INDEX idx_stock_movements_type_business_no (type, business_no)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stock_movements' AND index_name = 'idx_stock_movements_dimension_time') = 0,
    'ALTER TABLE stock_movements ADD INDEX idx_stock_movements_dimension_time (sku_id, warehouse_id, area_id, location_id, occurred_at)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- stock adjust order: manual inventory correction, DRAFT -> SUBMITTED -> COMPLETED (or CANCELLED from
-- either DRAFT/SUBMITTED). Approve+execute are merged into a single "confirm" step for this rollout.
create table if not exists stock_adjust_order (
    id bigint not null,
    adjust_no varchar(64) not null,
    status varchar(32) not null,
    reason_type varchar(32) not null,
    reason varchar(255),
    warehouse_id bigint not null,
    created_by varchar(64),
    confirmed_by varchar(64),
    confirmed_at timestamp null,
    cancelled_by varchar(64),
    cancelled_at timestamp null,
    cancel_reason varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    constraint uk_stock_adjust_order_no unique (adjust_no),
    index idx_stock_adjust_order_status_created (status, created_at)
);

create table if not exists stock_adjust_order_item (
    id bigint not null,
    adjust_order_id bigint not null,
    sku_id bigint not null,
    warehouse_id bigint not null,
    area_id bigint not null,
    location_id bigint not null,
    adjust_type varchar(16) not null,
    adjust_qty integer not null,
    before_on_hand_qty integer,
    after_on_hand_qty integer,
    before_locked_qty integer,
    after_locked_qty integer,
    before_frozen_qty integer,
    after_frozen_qty integer,
    before_available_qty integer,
    after_available_qty integer,
    remark varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    index idx_stock_adjust_order_item_order (adjust_order_id),
    index idx_stock_adjust_order_item_dimension (sku_id, warehouse_id, area_id, location_id)
);

-- stock count task: DRAFT -> COUNTING (book snapshot taken, locations locked to COUNTING) -> COMPLETED
-- (diffs applied, locations restored) or CANCELLED from either DRAFT/COUNTING. Submit+complete are
-- merged into a single "complete" step for this rollout; COUNTING covers "recording actuals".
create table if not exists stock_count_task (
    id bigint not null,
    count_no varchar(64) not null,
    warehouse_id bigint not null,
    area_id bigint,
    location_id bigint,
    status varchar(32) not null,
    remark varchar(255),
    created_by varchar(64),
    completed_by varchar(64),
    completed_at timestamp null,
    cancelled_by varchar(64),
    cancelled_at timestamp null,
    cancel_reason varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    constraint uk_stock_count_task_no unique (count_no),
    index idx_stock_count_task_status_created (status, created_at)
);

create table if not exists stock_count_item (
    id bigint not null,
    count_task_id bigint not null,
    sku_id bigint not null,
    warehouse_id bigint not null,
    area_id bigint not null,
    location_id bigint not null,
    book_on_hand_qty integer not null,
    book_locked_qty integer not null,
    book_frozen_qty integer not null,
    book_available_qty integer not null,
    actual_qty integer,
    diff_qty integer,
    status varchar(16) not null,
    remark varchar(255),
    enabled tinyint(1) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    index idx_stock_count_item_task (count_task_id),
    index idx_stock_count_item_dimension (sku_id, warehouse_id, area_id, location_id)
);

-- Anchor stock adjustments to a real inventory row instead of a free sku+location combination.
-- Nullable: only the off-book "create new inventory" increase mode is allowed to leave it unset.
-- Historical rows keep inventory_id = NULL; they are not backfilled and still display fine off of
-- their existing sku/warehouse/area/location snapshot columns.
SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'inventory_id') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN inventory_id BIGINT NULL COMMENT ''库存余额ID''', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND index_name = 'idx_stock_adjust_order_item_inventory') = 0,
    'ALTER TABLE stock_adjust_order_item ADD INDEX idx_stock_adjust_order_item_inventory (inventory_id)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- uk_inventory_dimension (warehouse_id, sku_id, area_id, location_id), added a few rounds ago above,
-- is what actually enforces "one balance row per dimension" — it already exists by the time this
-- comment is read (this file has run idempotently on every startup since). InventoryStatus is not
-- part of the key: it only ever takes NORMAL/EXCEPTION and is derived purely from area_id at
-- creation time (see Inventory's constructor), so it can never vary independently within a fixed
-- (warehouse,sku,area,location) tuple — folding it into the key would be redundant, not stricter.
-- To manually check for pre-existing duplicate dimensions before trusting this constraint on a new
-- environment, run:
--   SELECT sku_id, warehouse_id, area_id, location_id, COUNT(*) AS cnt
--   FROM inventory
--   GROUP BY sku_id, warehouse_id, area_id, location_id
--   HAVING COUNT(*) > 1;
-- (Not run automatically here: if it ever found rows, blindly merging or deleting them would be a
-- destructive, business-meaning-laden decision that belongs to a human, not a startup script.)

-- Unified business-number generator: one row per (biz_type, seq_date), current_value incremented
-- under SELECT ... FOR UPDATE by BizNoGeneratorService. New table, no historical-data risk.
create table if not exists biz_sequence (
    id bigint not null,
    biz_type varchar(64) not null,
    seq_date varchar(8) not null,
    prefix varchar(16) not null,
    current_value bigint not null,
    remark varchar(255),
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id),
    constraint uk_biz_sequence_type_date unique (biz_type, seq_date)
);

-- Normal-area <-> exception-area inventory transfer, modeled as a new stock_adjust_order_item
-- "action" instead of a separate module. adjust_action is nullable purely to tolerate rows written
-- before this column existed; the backfill UPDATE below immediately normalizes every such row (and
-- is itself idempotent: after the first run no row still has adjust_action IS NULL), so in practice
-- the column is always populated by the time any application code reads it.
SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'adjust_action') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN adjust_action VARCHAR(32) NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE stock_adjust_order_item SET adjust_action = 'QUANTITY_INCREASE' WHERE adjust_action IS NULL AND adjust_type = 'INCREASE';
UPDATE stock_adjust_order_item SET adjust_action = 'QUANTITY_DECREASE' WHERE adjust_action IS NULL AND adjust_type = 'DECREASE';

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'target_warehouse_id') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN target_warehouse_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'target_area_id') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN target_area_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'target_location_id') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN target_location_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'target_inventory_id') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN target_inventory_id BIGINT NULL', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'hold_qty') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN hold_qty INTEGER NOT NULL DEFAULT 0', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND column_name = 'hold_status') = 0,
    'ALTER TABLE stock_adjust_order_item ADD COLUMN hold_status VARCHAR(32) NOT NULL DEFAULT ''NONE''', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND index_name = 'idx_stock_adjust_item_target_location') = 0,
    'ALTER TABLE stock_adjust_order_item ADD INDEX idx_stock_adjust_item_target_location (target_location_id)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stmt := (SELECT IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stock_adjust_order_item' AND index_name = 'idx_stock_adjust_item_target_inventory') = 0,
    'ALTER TABLE stock_adjust_order_item ADD INDEX idx_stock_adjust_item_target_inventory (target_inventory_id)', 'SELECT 1'));
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Data dictionary: business-facing codes (operationType, bizType, ...) keep living as Java enums for
-- logic; this is purely the display layer (label/sort/tag color/enabled) so it can be edited by an
-- admin without a redeploy. New tables, no historical-data risk. Seed rows are inserted idempotently
-- by DictDataInitializer on startup, not here, matching how sys_permission/sys_role are seeded.
create table if not exists sys_dict_type (
    id bigint not null,
    dict_code varchar(64) not null,
    dict_name varchar(128) not null,
    status varchar(32) not null default 'ENABLED',
    remark varchar(255),
    sort_order int not null default 0,
    created_by varchar(64),
    created_at timestamp not null,
    updated_by varchar(64),
    updated_at timestamp not null,
    primary key (id),
    constraint uk_sys_dict_type_code unique (dict_code)
);

create table if not exists sys_dict_item (
    id bigint not null,
    dict_code varchar(64) not null,
    item_value varchar(128) not null,
    item_label varchar(128) not null,
    item_label_en varchar(128),
    sort_order int not null default 0,
    status varchar(32) not null default 'ENABLED',
    tag_type varchar(32),
    css_class varchar(128),
    is_system tinyint(1) not null default 0,
    remark varchar(255),
    created_by varchar(64),
    created_at timestamp not null,
    updated_by varchar(64),
    updated_at timestamp not null,
    deleted tinyint(1) not null default 0,
    primary key (id),
    constraint uk_sys_dict_item_code_value unique (dict_code, item_value),
    index idx_sys_dict_item_dict_code (dict_code),
    index idx_sys_dict_item_status (status)
);
