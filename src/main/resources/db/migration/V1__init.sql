CREATE TABLE categories
(
    id        UUID         NOT NULL,
    name      VARCHAR(255) NOT NULL,
    parent_id UUID,
    CONSTRAINT pk_categories PRIMARY KEY (id)
);

CREATE TABLE event_categories
(
    category_id UUID NOT NULL,
    event_id    UUID NOT NULL,
    CONSTRAINT pk_event_categories PRIMARY KEY (category_id, event_id)
);

CREATE TABLE event_cover_photos
(
    event_id  UUID NOT NULL,
    photo_url VARCHAR(255)
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

CREATE TABLE organizations
(
    id         UUID                        NOT NULL,
    name       VARCHAR(255)                NOT NULL,
    logo_url   VARCHAR(255),
    website    VARCHAR(255),
    user_id    VARCHAR(255)                NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_organizations PRIMARY KEY (id)
);

CREATE TABLE seating_layout_templates
(
    id              UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    layout_data     JSONB,
    CONSTRAINT pk_seating_layout_templates PRIMARY KEY (id)
);

CREATE TABLE session_seating_maps
(
    id               UUID NOT NULL,
    event_session_id UUID NOT NULL,
    layout_data      JSONB,
    CONSTRAINT pk_session_seating_maps PRIMARY KEY (id)
);

CREATE TABLE tiers
(
    id       UUID NOT NULL,
    name     VARCHAR(255),
    color    VARCHAR(255),
    price    DECIMAL,
    event_id UUID NOT NULL,
    CONSTRAINT pk_tiers PRIMARY KEY (id)
);

CREATE TABLE venue_facilities
(
    venue_id   UUID NOT NULL,
    facilities VARCHAR(255)
);

CREATE TABLE venues
(
    id              UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(255),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    capacity        INTEGER,
    organization_id UUID         NOT NULL,
    CONSTRAINT pk_venues PRIMARY KEY (id)
);

ALTER TABLE categories
    ADD CONSTRAINT uc_categories_name UNIQUE (name);

ALTER TABLE session_seating_maps
    ADD CONSTRAINT uc_session_seating_maps_event_session UNIQUE (event_session_id);

ALTER TABLE categories
    ADD CONSTRAINT FK_CATEGORIES_ON_PARENT FOREIGN KEY (parent_id) REFERENCES categories (id);

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organizations (id);

ALTER TABLE events
    ADD CONSTRAINT FK_EVENTS_ON_VENUE FOREIGN KEY (venue_id) REFERENCES venues (id);

ALTER TABLE event_sessions
    ADD CONSTRAINT FK_EVENT_SESSIONS_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE seating_layout_templates
    ADD CONSTRAINT FK_SEATING_LAYOUT_TEMPLATES_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organizations (id);

ALTER TABLE session_seating_maps
    ADD CONSTRAINT FK_SESSION_SEATING_MAPS_ON_EVENT_SESSION FOREIGN KEY (event_session_id) REFERENCES event_sessions (id);

ALTER TABLE tiers
    ADD CONSTRAINT FK_TIERS_ON_EVENT FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE venues
    ADD CONSTRAINT FK_VENUES_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organizations (id);

ALTER TABLE event_categories
    ADD CONSTRAINT fk_evecat_on_category FOREIGN KEY (category_id) REFERENCES categories (id);

ALTER TABLE event_categories
    ADD CONSTRAINT fk_evecat_on_event FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE event_cover_photos
    ADD CONSTRAINT fk_event_cover_photos_on_event FOREIGN KEY (event_id) REFERENCES events (id);

ALTER TABLE venue_facilities
    ADD CONSTRAINT fk_venue_facilities_on_venue FOREIGN KEY (venue_id) REFERENCES venues (id);