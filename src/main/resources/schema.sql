CREATE TABLE IF NOT EXISTS orders (
    id bigint not null auto_increment primary key,
    total_price bigint not null,
    status varchar(20) not null,
    member_id bigint not null,
    delivery_id bigint not null,
    created_date datetime(6) not null,
    updated_date datetime(6) not null
);

CREATE TABLE IF NOT EXISTS order_item (
    id bigint not null auto_increment primary key,
    order_id bigint not null,
    product_id bigint not null,
    product_name varchar(255) not null,
    product_price bigint not null
);
