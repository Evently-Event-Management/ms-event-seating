ALTER TABLE discounts
    ADD is_active BOOLEAN;

ALTER TABLE discounts
    ALTER COLUMN is_active SET NOT NULL;