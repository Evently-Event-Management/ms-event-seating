package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.validators.ValidSessionDuration;
import com.ticketly.mseventseating.validators.ValidSessionLocation;
import dto.SessionSeatingMapDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import model.SessionType;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidSessionLocation // ✅ Apply the custom class-level validator
@ValidSessionDuration(minMinutes = 30, maxHours = 12) // Validate session duration
public class SessionRequest {
    @NotNull
    @Future
    private OffsetDateTime startTime;

    @NotNull
    @Future
    private OffsetDateTime endTime;

    // ✅ UPDATED: Single sales start time field calculated by frontend
    @NotNull
    @Future
    private OffsetDateTime salesStartTime;

    @NotNull
    private SessionType sessionType;

    @NotNull // The object itself must not be null
    @Valid
    private VenueDetailsDTO venueDetails;

    @NotNull
    private SessionSeatingMapDTO layoutData;
}
