ALTER TABLE venue
    ADD address VARCHAR(255);

ALTER TABLE venue
    ADD organization_id UUID;

ALTER TABLE venue
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE venue
    ADD CONSTRAINT FK_VENUE_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organization (id);

ALTER TABLE venue
    DROP COLUMN has_seats;

ALTER TABLE venue
    DROP COLUMN layout_json;