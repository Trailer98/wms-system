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
    biz_no varchar(64),
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
