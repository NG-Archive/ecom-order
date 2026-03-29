CREATE TABLE order_item (
    id bigint not null auto_increment primary key,
    order_id bigint not null,
    product_id bigint not null,
    product_name varchar(255) not null,
    product_price bigint not null,
    product_quantity bigint not null
);
