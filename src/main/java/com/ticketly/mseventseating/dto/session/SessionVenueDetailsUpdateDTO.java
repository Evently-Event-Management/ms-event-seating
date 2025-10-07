package com.ticketly.mseventseating.dto.session;

import com.ticketly.mseventseating.dto.event.VenueDetailsDTO;
import com.ticketly.mseventseating.validators.ValidSessionLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating just the venue details of a session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidSessionLocation
public class SessionVenueDetailsUpdateDTO {
    
    @NotNull
    @Valid
    private VenueDetailsDTO venueDetails;
}
