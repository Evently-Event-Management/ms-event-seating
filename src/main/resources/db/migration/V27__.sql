ALTER TABLE organization_members
    ADD COLUMN is_active BOOLEAN;

-- Set all existing rows (which will currently be NULL) to true
UPDATE organization_members
SET is_active = TRUE
WHERE is_active IS NULL;

-- Then make the column NOT NULL
ALTER TABLE organization_members
    ALTER COLUMN is_active SET NOT NULL;
