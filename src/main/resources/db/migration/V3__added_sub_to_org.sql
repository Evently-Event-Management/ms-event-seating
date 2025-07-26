ALTER TABLE organization
    ADD updated_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE organization
    ADD user_id VARCHAR(255);

ALTER TABLE organization
    ALTER COLUMN user_id SET NOT NULL;