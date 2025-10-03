create or replace function validate_seat_statuses(p_session_id uuid, p_seat_ids uuid[]) returns integer
    language plpgsql
as
$$
DECLARE
    v_layout_data JSONB;
    v_unavailable_count INTEGER;
BEGIN
    -- 1. Get layout_data for the session
    SELECT layout_data
    INTO v_layout_data
    FROM session_seating_maps
    WHERE event_session_id = p_session_id;

    IF NOT FOUND THEN
        -- If no layout found, treat all as unavailable
        RETURN array_length(p_seat_ids, 1);
    END IF;

    -- 2. Calculate unavailable seats in a single query
    WITH all_seats AS (
        SELECT seat_element
        FROM jsonb_array_elements(v_layout_data -> 'layout' -> 'blocks') AS block,
             jsonb_array_elements(block -> 'seats') AS seat_element
        WHERE block -> 'seats' IS NOT NULL

        UNION ALL

        SELECT seat_element
        FROM jsonb_array_elements(v_layout_data -> 'layout' -> 'blocks') AS block,
             jsonb_array_elements(block -> 'rows') AS row,
             jsonb_array_elements(row -> 'seats') AS seat_element
        WHERE block -> 'rows' IS NOT NULL
    ), matched AS (
        SELECT (seat_element ->> 'id')::UUID AS id,
               (seat_element ->> 'status') AS status
        FROM all_seats
        WHERE (seat_element ->> 'id')::UUID = ANY(p_seat_ids)
    )
    SELECT
        -- unavailable = booked/reserved/etc
        COUNT(*) FILTER (WHERE status <> 'AVAILABLE')
            -- + seats that don’t exist at all
            + (array_length(p_seat_ids, 1) - COUNT(*))
    INTO v_unavailable_count
    FROM matched;

    RETURN v_unavailable_count;
END;
$$;

alter function validate_seat_statuses(uuid, uuid[]) owner to ticketly;

