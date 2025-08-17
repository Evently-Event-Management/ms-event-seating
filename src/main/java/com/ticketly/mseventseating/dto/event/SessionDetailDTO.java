package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.model.SalesStartRuleType;
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
public class SessionDetailDTO {
    private UUID id;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private SessionType sessionType;
    private VenueDetailsDTO venueDetails;
    private SalesStartRuleType salesStartRuleType;
    private Integer salesStartHoursBefore;
    private OffsetDateTime salesStartFixedDatetime;
    private SessionStatus status;
    private SessionSeatingMapDTO layoutData;
}
