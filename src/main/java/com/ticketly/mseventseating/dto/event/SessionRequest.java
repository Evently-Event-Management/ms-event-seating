package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.model.SalesStartRuleType;
import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Data
@Builder
public class SessionRequest {
    @NotNull
    @Future
    private OffsetDateTime startTime;
    @NotNull
    @Future
    private OffsetDateTime endTime;
    @NotNull
    private SalesStartRuleType salesStartRuleType;
    private Integer salesStartHoursBefore;
    private OffsetDateTime salesStartFixedDatetime;

    // ✅ ADDED: All location information is now session-specific.
    @NotNull
    private boolean isOnline;
    private String onlineLink;
    private VenueDetailsDTO venueDetails; // Can be null if isOnline is true

    @NotNull
    private SessionSeatingMapRequest layoutData;
}
