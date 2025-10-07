package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.validators.ValidSalesStartTime;
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
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidSessionLocation // ✅ Apply the custom class-level validator
@ValidSessionDuration(minMinutes = 30, maxHours = 12)
@ValidSalesStartTime
public class SessionRequest {
    @NotNull
    private UUID id;

    @NotNull
    @Future
    private OffsetDateTime startTime;

    @NotNull
    @Future
    private OffsetDateTime endTime;

    @NotNull
    @Future
    private OffsetDateTime salesStartTime;

    @NotNull
    private SessionType sessionType;

    @NotNull
    @Valid
    private VenueDetailsDTO venueDetails;

    @NotNull
    private SessionSeatingMapDTO layoutData;
}
