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

ALTER TABLE categories
    ADD CONSTRAINT uc_categories_name UNIQUE (name);

ALTER TABLE categories
    ADD CONSTRAINT FK_CATEGORIES_ON_PARENT FOREIGN KEY (parent_id) REFERENCES categories (id);

ALTER TABLE event_categories
    ADD CONSTRAINT fk_evecat_on_category FOREIGN KEY (category_id) REFERENCES categories (id);

ALTER TABLE event_categories
    ADD CONSTRAINT fk_evecat_on_event FOREIGN KEY (event_id) REFERENCES event (id);