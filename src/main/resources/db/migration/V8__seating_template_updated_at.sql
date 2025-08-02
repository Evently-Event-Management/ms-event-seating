ALTER TABLE seating_layout_templates
    ADD updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Set updated_at for existing records to current timestamp
UPDATE seating_layout_templates SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

-- (Optional) Remove default if you want to set updated_at explicitly in application code
ALTER TABLE seating_layout_templates ALTER COLUMN updated_at DROP DEFAULT;