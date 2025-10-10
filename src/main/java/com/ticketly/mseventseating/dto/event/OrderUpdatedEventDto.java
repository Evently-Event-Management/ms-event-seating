package com.ticketly.mseventseating.dto.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderUpdatedEventDto(
        UUID OrderID,
        UUID UserID,
        UUID SessionID,
        UUID EventID,
        String Status,
        BigDecimal SubTotal,
        UUID DiscountID,
        String DiscountCode,
        BigDecimal DiscountAmount,
        BigDecimal Price,
        OffsetDateTime CreatedAt,
        List<TicketDto> tickets
) {
    public record TicketDto(
            UUID ticket_id,
            UUID order_id,
            UUID seat_id,
            String seat_label,
            String colour,
            UUID tier_id,
            String tier_name,
            BigDecimal price_at_purchase,
            OffsetDateTime issued_at,
            boolean checked_in,
            OffsetDateTime checked_in_time
    ) {}
}