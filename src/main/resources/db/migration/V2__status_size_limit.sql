ALTER TABLE event
    DROP COLUMN status;

ALTER TABLE event
    ADD status VARCHAR(20);

ALTER TABLE seat_map
    DROP COLUMN status;

ALTER TABLE seat_map
    ADD status VARCHAR(20);