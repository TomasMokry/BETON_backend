-- Bank fee for card payments: total_price stays what the customer paid,
-- net_price = total_price - card_fee is what actually reaches the account.
ALTER TABLE orders
    ADD COLUMN card_fee  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN net_price DECIMAL(10, 2) NULL;

-- Existing orders have no recorded fee
UPDATE orders SET net_price = total_price WHERE net_price IS NULL;
