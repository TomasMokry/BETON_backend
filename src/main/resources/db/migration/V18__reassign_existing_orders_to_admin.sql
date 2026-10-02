-- One-off data fix: before per-user orders, every order was stored under user 1.
-- Move all orders that exist at this point to the admin user (id 2).
-- The EXISTS guard turns this into a no-op on a database without user 2 instead of failing the FK.
UPDATE orders
SET customer_id = 2
WHERE customer_id <> 2
  AND EXISTS (SELECT 1 FROM users WHERE id = 2);
