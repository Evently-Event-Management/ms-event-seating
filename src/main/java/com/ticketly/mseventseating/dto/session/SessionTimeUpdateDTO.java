package com.ticketly.mseventseating.dto.session;

import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import com.ticketly.mseventseating.validators.ValidSalesStartTime;
import com.ticketly.mseventseating.validators.ValidSessionDuration;
import com.ticketly.mseventseating.validators.ValidSessionLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionType;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidSessionLocation
@ValidSessionDuration(minMinutes = 30, maxHours = 12)
@ValidSalesStartTime
public class SessionTimeUpdateDTO {
    
    @NotNull
    @Future
    private OffsetDateTime startTime;

    @NotNull
    @Future
    private OffsetDateTime endTime;

    @NotNull
    @Future
    private OffsetDateTime salesStartTime;

//    @NotNull
//    private SessionType sessionType;
//
//    @NotNull
//    @Valid
//    private VenueDetailsDTO venueDetails;    @NotNull
    private SessionType sessionType;

    @NotNull
    @Valid
    private VenueDetailsDTO venueDetails;
}