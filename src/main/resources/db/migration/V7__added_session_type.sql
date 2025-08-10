ALTER TABLE event_sessions
    ADD session_type VARCHAR(255);

ALTER TABLE event_sessions
    ALTER COLUMN session_type SET NOT NULL;

ALTER TABLE event_sessions
    DROP COLUMN is_online;

ALTER TABLE event_sessions
    DROP COLUMN online_link;