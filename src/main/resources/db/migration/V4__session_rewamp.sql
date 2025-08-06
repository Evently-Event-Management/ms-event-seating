ALTER TABLE event_categories
    DROP CONSTRAINT fk_evecat_on_category;

ALTER TABLE event_categories
    DROP CONSTRAINT fk_evecat_on_event;

ALTER TABLE events
    DROP CONSTRAINT fk_events_on_venue;

ALTER TABLE venue_facilities
    DROP CONSTRAINT fk_venue_facilities_on_venue;

ALTER TABLE venues
    DROP CONSTRAINT fk_venues_on_organization;

ALTER TABLE events
    ADD category_id UUID;

ALTER TABLE event_sessions
    ADD is_online BOOLEAN;

ALTER TABLE event_sessions
    ADD online_link VARCHAR(255);

ALTER TABLE event_sessions
    ADD venue_details JSONB;

ALTER TABLE event_sessions
    ALTER COLUMN is_online SET NOT NULL;

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_CATEGORY FOREIGN KEY (category_id) REFERENCES categories (id);

DROP TABLE event_categories CASCADE;

DROP TABLE venue_facilities CASCADE;

DROP TABLE venues CASCADE;

ALTER TABLE events
    DROP COLUMN is_online;

ALTER TABLE events
    DROP COLUMN location_description;

ALTER TABLE events
    DROP COLUMN online_link;

ALTER TABLE events
    DROP COLUMN venue_id;