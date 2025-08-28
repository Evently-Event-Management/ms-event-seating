CREATE OR REPLACE FUNCTION update_seat_statuses(
    p_session_id UUID,
    p_seat_ids UUID[],
    p_new_status TEXT
)
    RETURNS VOID AS
$$
DECLARE
    v_current_layout JSONB;
    v_updated_layout JSONB;
    v_seat_id        UUID;
    v_seat_path      TEXT[];
BEGIN
    -- 1. Select the current layout_data for the given session
    SELECT layout_data
    INTO v_current_layout
    FROM session_seating_maps
    WHERE event_session_id = p_session_id;

    -- If no record found, exit
    IF NOT FOUND THEN
        RETURN;
    END IF;

    v_updated_layout := v_current_layout;

    -- 2. Loop through each seat ID that needs to be updated
    FOREACH v_seat_id IN ARRAY p_seat_ids
        LOOP
        -- 3. Find the JSON path to the target seat
        -- This query searches through blocks->seats and blocks->rows->seats
            SELECT path
            INTO v_seat_path
            FROM (
                     -- Search in seats directly under blocks
                     SELECT ARRAY ['layout', 'blocks', (block.idx - 1)::TEXT, 'seats', (seat.idx - 1)::TEXT]
                     FROM jsonb_array_elements(v_current_layout -> 'layout' -> 'blocks') WITH ORDINALITY AS block(elem, idx),
                          jsonb_array_elements(block.elem -> 'seats') WITH ORDINALITY AS seat(elem, idx)
                     WHERE (seat.elem ->> 'id')::UUID = v_seat_id
                     UNION ALL
                     -- Search in seats under blocks->rows
                     SELECT ARRAY ['layout', 'blocks', (block.idx - 1)::TEXT, 'rows', (row.idx - 1)::TEXT, 'seats', (seat.idx - 1)::TEXT]
                     FROM jsonb_array_elements(v_current_layout -> 'layout' -> 'blocks') WITH ORDINALITY AS block(elem, idx),
                          jsonb_array_elements(block.elem -> 'rows') WITH ORDINALITY AS row(elem, idx),
                          jsonb_array_elements(row.elem -> 'seats') WITH ORDINALITY AS seat(elem, idx)
                     WHERE (seat.elem ->> 'id')::UUID = v_seat_id) AS paths(path)
            LIMIT 1;

            -- 4. If a path was found, update the status using jsonb_set
            IF v_seat_path IS NOT NULL THEN
                v_updated_layout := jsonb_set(
                        v_updated_layout,
                        v_seat_path || 'status',
                        to_jsonb(p_new_status),
                        false
                                    );
            END IF;
        END LOOP;

    -- 5. Update the table with the modified JSONB
    UPDATE session_seating_maps
    SET layout_data = v_updated_layout
    WHERE event_session_id = p_session_id;

END;
$$ LANGUAGE plpgsql;