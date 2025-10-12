package com.ticketly.mseventseating.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionAnalyticsResponse {
    private UUID organizationId;
    private String organizationName;
    private UUID eventId; // Optional - will be null when getting analytics for an organization
    private String eventTitle; // Optional - will be null when getting analytics for an organization
    private Long totalSessions;
    private List<SessionStatusCountDTO> sessionsByStatus;
}