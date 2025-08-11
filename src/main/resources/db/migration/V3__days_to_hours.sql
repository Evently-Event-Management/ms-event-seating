ALTER TABLE event_sessions
    ADD sales_start_fixed_datetime TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE event_sessions
    ADD sales_start_hours_before INTEGER;

ALTER TABLE event_sessions
    ADD sales_start_rule_type VARCHAR(255);

ALTER TABLE event_sessions
    ALTER COLUMN sales_start_rule_type SET NOT NULL;

ALTER TABLE events
    DROP COLUMN sales_start_days_before;

ALTER TABLE events
    DROP COLUMN sales_start_fixed_datetime;

ALTER TABLE events
    DROP COLUMN sales_start_rule_type;