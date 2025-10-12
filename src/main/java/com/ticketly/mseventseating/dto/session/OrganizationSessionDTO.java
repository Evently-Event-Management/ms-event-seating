package com.ticketly.mseventseating.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.EventStatus;
import model.SessionStatus;
import model.SessionType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationSessionDTO {
    // Session details
    private UUID sessionId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime salesStartTime;
    private SessionType sessionType;
    private SessionStatus sessionStatus;
    
    // Event details
    private UUID eventId;
    private String eventTitle;
    private EventStatus eventStatus;
    private String categoryName;
}