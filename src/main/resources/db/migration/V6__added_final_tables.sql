ALTER TABLE event_categories
    DROP CONSTRAINT fk_evecat_on_event;

ALTER TABLE event_cover_photos
    DROP CONSTRAINT fk_event_cover_photos_on_event;

ALTER TABLE event
    DROP CONSTRAINT fk_event_on_organization;

ALTER TABLE event
    DROP CONSTRAINT fk_event_on_venue;

ALTER TABLE seat_map
    DROP CONSTRAINT fk_seat_map_on_event;

ALTER TABLE tier
    DROP CONSTRAINT fk_tier_on_event;

CREATE TABLE event_seating_maps
(
    id          UUID NOT NULL,
    event_id    UUID NOT NULL,
    layout_data JSONB,
    CONSTRAINT pk_event_seating_maps PRIMARY KEY (id)
);

CREATE TABLE event_sessions
(
    id         UUID                        NOT NULL,
    event_id   UUID                        NOT NULL,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status     VARCHAR(255)                NOT NULL,
    CONSTRAINT pk_event_sessions PRIMARY KEY (id)
);

CREATE TABLE events
(
    id                         UUID                        NOT NULL,
    organization_id            UUID                        NOT NULL,
    venue_id                   UUID,
    title                      VARCHAR(255)                NOT NULL,
    description                TEXT,
    overview                   TEXT,
    status                     VARCHAR(255)                NOT NULL,
    rejection_reason           VARCHAR(255),
    is_online                  BOOLEAN                     NOT NULL,
    online_link                VARCHAR(255),
    location_description       VARCHAR(255),
    sales_start_rule_type      VARCHAR(255)                NOT NULL,
    sales_start_days_before    INTEGER,
    sales_start_fixed_datetime TIMESTAMP WITHOUT TIME ZONE,
    created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY (id)
);

CREATE TABLE seating_layout_templates
(
    id              UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    layout_data     JSONB,
    CONSTRAINT pk_seating_layout_templates PRIMARY KEY (id)
);

ALTER TABLE event_cover_photos
    ADD photo_url VARCHAR(255);

ALTER TABLE event_seating_maps
    ADD CONSTRAINT uc_event_seating_maps_event UNIQUE (event_id);

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_VENUE FOREIGN KEY (venue_id) REFERENCES venue (id);

ALTER TABLE event_seating_maps
    ADD CONSTRAINT FK_EVENT_SEATING_MAPS_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE event_sessions
    ADD CONSTRAINT FK_EVENT_SESSIONS_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE seating_layout_templates
    ADD CONSTRAINT FK_SEATING_LAYOUT_TEMPLATES_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE seat_map
    ADD CONSTRAINT FK_SEAT_MAP_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE tier
    ADD CONSTRAINT FK_TIER_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE event_categories
    ADD CONSTRAINT fk_evecat_on_event FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE event_cover_photos
    ADD CONSTRAINT fk_event_cover_photos_on_event FOREIGN KEY (event_id) REFERENCES events (id);

DROP TABLE event CASCADE;

ALTER TABLE event_cover_photos
    DROP COLUMN cover_photos;