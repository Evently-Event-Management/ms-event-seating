ALTER TABLE discounts
    ADD event_id UUID;

ALTER TABLE discounts
    ALTER COLUMN event_id SET NOT NULL;

ALTER TABLE discounts
    ADD CONSTRAINT FK_DISCOUNTS_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);