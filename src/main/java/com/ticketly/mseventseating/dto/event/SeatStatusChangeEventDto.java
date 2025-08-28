package com.ticketly.mseventseating.dto.event;

import java.util.List;
import java.util.UUID;

// Using a record for an immutable, concise DTO
public record SeatStatusChangeEventDto(
        UUID sessionId,
        List<UUID> seatIds
) {
}