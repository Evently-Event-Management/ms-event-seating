CREATE TABLE discounts
(
    id            UUID         NOT NULL,
    session_id    UUID         NOT NULL,
    code          VARCHAR(255) NOT NULL,
    type          VARCHAR(255) NOT NULL,
    parameters    JSONB,
    max_usage     INTEGER,
    current_usage INTEGER      NOT NULL,
    is_public     BOOLEAN      NOT NULL,
    active_from   TIMESTAMP WITHOUT TIME ZONE,
    expires_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_discounts PRIMARY KEY (id)
);

ALTER TABLE discounts
    ADD CONSTRAINT uc_discounts_code UNIQUE (code);

ALTER TABLE discounts
    ADD CONSTRAINT FK_DISCOUNTS_ON_SESSION FOREIGN KEY (session_id) REFERENCES event_sessions (id);