ALTER TABLE event_sessions
    ADD sales_start_time TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE event_sessions
    DROP COLUMN sales_start_fixed_datetime;

ALTER TABLE event_sessions
    DROP COLUMN sales_start_hours_before;

ALTER TABLE event_sessions
    DROP COLUMN sales_start_rule_type;