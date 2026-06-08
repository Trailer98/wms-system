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

create table if not exists inventory (
    id bigint not null,
    warehouse_id bigint not null,
    sku_id bigint not null,
    quantity integer not null,
    reserved_quantity integer not null,
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
    supplier_name varchar(128),
    received_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists inbound_order_items (
    id bigint not null,
    order_id bigint not null,
    sku_id bigint not null,
    quantity integer not null,
    primary key (id)
);

create table if not exists outbound_orders (
    id bigint not null,
    order_no varchar(64) not null unique,
    status varchar(32) not null,
    warehouse_id bigint not null,
    customer_name varchar(128),
    shipped_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    primary key (id)
);

create table if not exists outbound_order_items (
    id bigint not null,
    order_id bigint not null,
    sku_id bigint not null,
    quantity integer not null,
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
    primary key (id)
);
