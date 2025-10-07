package com.ticketly.mseventseating.dto.session;

import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import com.ticketly.mseventseating.validators.ValidSessionLocation;
import dto.SessionSeatingMapDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidSessionLocation
public class SessionVenueUpdateDTO {
    
    @NotNull
    private SessionType sessionType;
    
    @NotNull
    @Valid
    private VenueDetailsDTO venueDetails;
    
    @NotNull
    private SessionSeatingMapDTO layoutData;
}