-- This migration sets the REPLICA IDENTITY to FULL for tables that
-- are critical for Debezium's Change Data Capture (CDC) process.
-- This ensures that DELETE events in the Kafka log contain the complete
-- "before" state of the row, which is necessary for the projection service.

ALTER TABLE event_cover_photos
    REPLICA IDENTITY FULL;

-- You should also consider adding this for any other join tables
-- or tables where the "before" state is important on delete.