ALTER TABLE seat_map
    DROP CONSTRAINT fk_seat_map_on_event;

ALTER TABLE seat_map
    DROP CONSTRAINT fk_seat_map_on_tier;

DROP TABLE seat_map CASCADE;