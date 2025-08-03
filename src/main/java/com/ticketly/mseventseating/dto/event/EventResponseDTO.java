package com.ticketly.mseventseating.dto.event;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class EventResponseDTO {
    private UUID id;
    private String title;
    private String status;
    private UUID organizationId;
    private OffsetDateTime createdAt;
}
