package com.ticketly.mseventseating.validators;

import com.ticketly.mseventseating.dto.session.SessionVenueDetailsUpdateDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * A validator that checks if the venue details in SessionVenueDetailsUpdateDTO are valid.
 * Note: This validator only checks if venueDetails is not null, as the session type information
 * is not available at validation time. The actual business logic validation for online/physical
 * sessions should be done in the service layer.
 */
public class SessionVenueDetailsValidator implements ConstraintValidator<ValidSessionLocation, SessionVenueDetailsUpdateDTO> {

    @Override
    public boolean isValid(SessionVenueDetailsUpdateDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true; // Let @NotNull handle this on the field itself.
        }

        // We can only validate that venueDetails is not null here
        // The actual validation of the venue details based on session type needs to happen in the service
        if (dto.getVenueDetails() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Venue details cannot be null.")
                    .addPropertyNode("venueDetails").addConstraintViolation();
            return false;
        }

        return true; // Basic validation passes, detailed validation will be in service
    }
}