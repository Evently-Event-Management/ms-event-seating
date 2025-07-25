CREATE TABLE event
(
    id                   UUID                        NOT NULL,
    title                VARCHAR(255)                NOT NULL,
    description          VARCHAR(2048),
    overview             VARCHAR(4096),
    start_time           TIMESTAMP WITHOUT TIME ZONE,
    end_time             TIMESTAMP WITHOUT TIME ZONE,
    is_online            BOOLEAN                     NOT NULL,
    online_link          VARCHAR(255),
    location_description VARCHAR(255),
    status               VARCHAR(255),
    rejection_reason     VARCHAR(255),
    organization_id      UUID                        NOT NULL,
    venue_id             UUID,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_event PRIMARY KEY (id)
);

CREATE TABLE event_cover_photos
(
    event_id     UUID NOT NULL,
    cover_photos VARCHAR(255)
);

CREATE TABLE organization
(
    id         UUID                        NOT NULL,
    name       VARCHAR(255)                NOT NULL,
    logo_url   VARCHAR(255),
    website    VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_organization PRIMARY KEY (id)
);

CREATE TABLE seat_map
(
    id        UUID NOT NULL,
    seat_name VARCHAR(255),
    event_id  UUID,
    tier_id   UUID,
    status    VARCHAR(255),
    CONSTRAINT pk_seat_map PRIMARY KEY (id)
);

CREATE TABLE tier
(
    id       UUID NOT NULL,
    name     VARCHAR(255),
    color    VARCHAR(255),
    price    DECIMAL,
    event_id UUID NOT NULL,
    CONSTRAINT pk_tier PRIMARY KEY (id)
);

CREATE TABLE venue
(
    id          UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    capacity    INTEGER,
    has_seats   BOOLEAN      NOT NULL,
    layout_json JSONB,
    CONSTRAINT pk_venue PRIMARY KEY (id)
);

CREATE TABLE venue_facilities
(
    venue_id   UUID NOT NULL,
    facilities VARCHAR(255)
);

ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_VENUE FOREIGN KEY (venue_id) REFERENCES venue (id);

ALTER TABLE seat_map
    ADD CONSTRAINT FK_SEAT_MAP_ON_EVENT FOREIGN KEY (event_id) REFERENCES event (id);

ALTER TABLE seat_map
    ADD CONSTRAINT FK_SEAT_MAP_ON_TIER FOREIGN KEY (tier_id) REFERENCES tier (id);

ALTER TABLE tier
    ADD CONSTRAINT FK_TIER_ON_EVENT FOREIGN KEY (event_id) REFERENCES event (id);

ALTER TABLE event_cover_photos
    ADD CONSTRAINT fk_event_cover_photos_on_event FOREIGN KEY (event_id) REFERENCES event (id);

ALTER TABLE venue_facilities
    ADD CONSTRAINT fk_venue_facilities_on_venue FOREIGN KEY (venue_id) REFERENCES venue (id);