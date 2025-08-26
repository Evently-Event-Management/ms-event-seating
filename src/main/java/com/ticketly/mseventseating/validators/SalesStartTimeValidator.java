package com.ticketly.mseventseating.validators;

import com.ticketly.mseventseating.dto.event.SessionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link ValidSalesStartTime} annotation.
 * Validates that the sales start time is before the session start time.
 */
public class SalesStartTimeValidator implements ConstraintValidator<ValidSalesStartTime, SessionRequest> {

    @Override
    public boolean isValid(SessionRequest session, ConstraintValidatorContext context) {
        // Skip validation if essential fields are null - let @NotNull handle these cases
        if (session == null || session.getStartTime() == null || session.getEndTime() == null
                || session.getSalesStartTime() == null) {
            return true;
        }

        // Initialize as valid
        boolean isValid = true;
        context.disableDefaultConstraintViolation();

        // Validate that end time is after start time
        if (!session.getEndTime().isAfter(session.getStartTime())) {
            context.buildConstraintViolationWithTemplate(
                            "Session end time must be after start time")
                    .addPropertyNode("endTime")
                    .addConstraintViolation();
            isValid = false;
        }

        // Validate that sales start time is before session start time
        if (!session.getSalesStartTime().isBefore(session.getStartTime())) {
            context.buildConstraintViolationWithTemplate(
                            "Sales start time must be before the session start time")
                    .addPropertyNode("salesStartTime")
                    .addConstraintViolation();
            isValid = false;
        }
        
        return isValid;
    }
}
