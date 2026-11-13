-- Schema inicial do sistema de gestão empresarial.
-- Todas as alterações estruturais são gerenciadas exclusivamente pelo Flyway.

create table store (
    id uuid primary key,
    name varchar(120) not null,
    document varchar(20),
    active boolean not null default true,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create table app_user (
    id uuid primary key,
    name varchar(120) not null,
    email varchar(160) not null,
    password_hash varchar(100) not null,
    role varchar(20) not null,
    active boolean not null default true,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create unique index ux_app_user_email on app_user (lower(email));

create table user_store (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    store_id uuid not null references store (id),
    created_at timestamp(6) with time zone not null,
    unique (user_id, store_id)
);

create index ix_user_store_user on user_store (user_id);

create table product (
    id uuid primary key,
    store_id uuid not null references store (id),
    name varchar(80) not null,
    normalized_name varchar(80) not null,
    sku varchar(40),
    category varchar(60) not null,
    description varchar(500),
    quantity integer not null check (quantity >= 0),
    minimum integer not null check (minimum >= 0),
    unit varchar(10) not null,
    active boolean not null default true,
    version bigint not null default 0,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create unique index ux_product_store_normalized_name
    on product (store_id, normalized_name);
create unique index ux_product_store_sku
    on product (store_id, lower(sku))
    where sku is not null;
create index ix_product_store_active on product (store_id, active);
create index ix_product_store_category on product (store_id, category);

create table stock_movement (
    id uuid primary key,
    store_id uuid not null references store (id),
    product_id uuid not null references product (id),
    product_name_snapshot varchar(80) not null,
    type varchar(20) not null,
    quantity integer not null check (quantity > 0),
    previous_quantity integer not null check (previous_quantity >= 0),
    resulting_quantity integer not null check (resulting_quantity >= 0),
    reason varchar(120) not null,
    observation varchar(500),
    user_id uuid not null references app_user (id),
    created_at timestamp(6) with time zone not null
);

create index ix_movement_store_created on stock_movement (store_id, created_at desc);
create index ix_movement_product_created on stock_movement (product_id, created_at desc);
create index ix_movement_type on stock_movement (store_id, type);

create table customer (
    id uuid primary key,
    store_id uuid not null references store (id),
    name varchar(120) not null,
    normalized_name varchar(120) not null,
    cpf varchar(14),
    phone varchar(20),
    email varchar(160),
    active boolean not null default true,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null
);

create index ix_customer_store_active on customer (store_id, active);
create index ix_customer_store_name on customer (store_id, normalized_name);

create table sale (
    id uuid primary key,
    store_id uuid not null references store (id),
    customer_id uuid references customer (id),
    user_id uuid not null references app_user (id),
    total numeric(14, 2) not null default 0,
    status varchar(20) not null,
    created_at timestamp(6) with time zone not null
);

create index ix_sale_store_created on sale (store_id, created_at desc);
create index ix_sale_customer on sale (customer_id, created_at desc);

create table sale_item (
    id uuid primary key,
    sale_id uuid not null references sale (id) on delete cascade,
    product_id uuid not null references product (id),
    product_name_snapshot varchar(80) not null,
    quantity integer not null check (quantity > 0),
    unit_price numeric(14, 2),
    subtotal numeric(14, 2),
    created_at timestamp(6) with time zone not null
);

create index ix_sale_item_sale on sale_item (sale_id);
