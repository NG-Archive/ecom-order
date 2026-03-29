CREATE TABLE orders (
    id bigint not null auto_increment primary key,
    total_price bigint not null,
    status varchar(20) not null,
    member_id bigint not null,
    delivery_id bigint not null,
    order_token varchar(50) not null,
    created_date datetime(6) not null,
    updated_date datetime(6) not null,

    constraint uk_orders_order_token unique (order_token)
);
