ALTER TABLE event_cover_photos
    ADD id UUID;

ALTER TABLE event_cover_photos
    ADD CONSTRAINT pk_event_cover_photos PRIMARY KEY (id);

ALTER TABLE event_cover_photos
    ALTER COLUMN photo_url SET NOT NULL;