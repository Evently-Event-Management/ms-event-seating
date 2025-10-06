package com.ticketly.mseventseating.dto.session;

import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import dto.SessionSeatingMapDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionStatus;
import model.SessionType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionResponse {
    private UUID id;
    private UUID eventId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime salesStartTime;
    private SessionType sessionType;
    private SessionStatus status;
    private VenueDetailsDTO venueDetails;
    private SessionSeatingMapDTO layoutData;
}