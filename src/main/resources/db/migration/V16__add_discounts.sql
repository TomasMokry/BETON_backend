ALTER TABLE cart_items
    ADD COLUMN discount_percent INT NOT NULL DEFAULT 0;

ALTER TABLE carts
    ADD COLUMN discount_percent INT NOT NULL DEFAULT 0;

ALTER TABLE order_items
    ADD COLUMN discount_percent INT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN discount_percent INT NOT NULL DEFAULT 0,
    ADD COLUMN subtotal_price DECIMAL(10, 2) NULL;

UPDATE orders SET subtotal_price = total_price WHERE subtotal_price IS NULL;
