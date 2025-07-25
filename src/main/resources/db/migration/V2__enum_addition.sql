-- Create enum types for the status columns
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'event_status') THEN
        CREATE TYPE event_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'seat_status') THEN
        CREATE TYPE seat_status AS ENUM ('AVAILABLE', 'RESERVED', 'BOOKED');
    END IF;
END $$;

-- Update the status columns to use the enum types
ALTER TABLE event
    ALTER COLUMN status TYPE event_status USING status::event_status;

ALTER TABLE seat_map
    ALTER COLUMN status TYPE seat_status USING status::seat_status;

-- Add default values
ALTER TABLE event
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE seat_map
    ALTER COLUMN status SET DEFAULT 'AVAILABLE';
