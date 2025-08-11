package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.model.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventSummaryDTO {
    private UUID id;
    private String title;
    private EventStatus status;
    private String organizationName;
    private UUID organizationId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String description;
    private String coverPhoto; // Just the first cover photo for the summary
    private int sessionCount;
    private OffsetDateTime earliestSessionDate;
}
