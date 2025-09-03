package com.ticketly.mseventseating.dto.event;

import java.util.List;
import java.util.UUID;

// Using a record for an immutable, concise DTO
public record SeatStatusChangeEventDto(
        UUID session_id,
        List<UUID> seat_ids
) {
}