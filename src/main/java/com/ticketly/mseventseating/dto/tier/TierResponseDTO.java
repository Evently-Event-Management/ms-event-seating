package com.ticketly.mseventseating.dto.tier;

import java.math.BigDecimal;
import java.util.UUID;

public record TierResponseDTO(
    UUID id,
    String name,
    String color,
    BigDecimal price,
    UUID eventId
) {
}