package com.ticketly.mseventseating.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ticketly.mseventseating.model.SalesStartRuleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor      // ✅ ADDED
@AllArgsConstructor     // ✅ ADDED
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

    @NotNull
    @JsonProperty("isOnline") // ✅ The Fix: Explicitly name the JSON property
    private boolean isOnline;

    private String onlineLink;
    private VenueDetailsDTO venueDetails;

    @NotNull
    private SessionSeatingMapDTO layoutData;
}
