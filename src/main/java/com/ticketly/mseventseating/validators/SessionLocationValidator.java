package com.ticketly.mseventseating.validators;

import com.ticketly.mseventseating.dto.event.SessionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import model.SessionType;

public class SessionLocationValidator implements ConstraintValidator<ValidSessionLocation, SessionRequest> {

    @Override
    public boolean isValid(SessionRequest sessionRequest, ConstraintValidatorContext context) {
        if (sessionRequest == null) {
            return true; // Let @NotNull handle this on the field itself.
        }

        if (sessionRequest.getSessionType() == SessionType.ONLINE) {
            // For ONLINE sessions, the onlineLink inside venueDetails must be present and not blank.
            if (sessionRequest.getVenueDetails() == null ||
                    sessionRequest.getVenueDetails().getOnlineLink() == null ||
                    sessionRequest.getVenueDetails().getOnlineLink().isBlank()) {

                // Add a specific error message to the 'venueDetails.onlineLink' field
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("An online link is required for online sessions.")
                        .addPropertyNode("venueDetails").addPropertyNode("onlineLink").addConstraintViolation();
                return false;
            }
        } else if (sessionRequest.getSessionType() == SessionType.PHYSICAL) {
            // For PHYSICAL sessions, the name inside venueDetails must be present and not blank.
            if (sessionRequest.getVenueDetails() == null ||
                    sessionRequest.getVenueDetails().getName() == null ||
                    sessionRequest.getVenueDetails().getName().isBlank()) {

                // Add a specific error message to the 'venueDetails.name' field
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("A venue name is required for physical sessions.")
                        .addPropertyNode("venueDetails").addPropertyNode("name").addConstraintViolation();
                return false;
            }
        }

        return true; // Validation passes
    }
}
