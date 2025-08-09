package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.model.SalesStartRuleType;
import com.ticketly.mseventseating.model.SessionType;
import com.ticketly.mseventseating.validators.ValidSessionLocation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidSessionLocation // ✅ Apply the custom class-level validator
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
    private SessionType sessionType;

    @NotNull // The object itself must not be null
    @Valid
    private VenueDetailsDTO venueDetails;

    @NotNull
    private SessionSeatingMapDTO layoutData;
}

