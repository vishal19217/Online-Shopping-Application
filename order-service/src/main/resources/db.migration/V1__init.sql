CREATE TABLE IF NOT EXISTS t_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_order_line_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sku_code VARCHAR(255),
    price DECIMAL(19, 2),
    quantity INT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_order_order_line_items_list (
    order_id BIGINT NOT NULL,
    order_line_items_list_id BIGINT NOT NULL,
    PRIMARY KEY (order_id, order_line_items_list_id),
    CONSTRAINT fk_order_line_items_list_order
        FOREIGN KEY (order_id) REFERENCES t_orders (id),
    CONSTRAINT fk_order_line_items_list_item
        FOREIGN KEY (order_line_items_list_id) REFERENCES t_order_line_items (id),
    CONSTRAINT uk_order_line_items_list_item
        UNIQUE (order_line_items_list_id)
);
