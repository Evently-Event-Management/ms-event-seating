-- 1. Create the table
CREATE TABLE debezium_heartbeat (
                                    id TEXT PRIMARY KEY,
                                    ts TIMESTAMPTZ
);

-- 2. Insert the single row that Debezium will update
INSERT INTO debezium_heartbeat (id, ts) VALUES ('1', NOW());