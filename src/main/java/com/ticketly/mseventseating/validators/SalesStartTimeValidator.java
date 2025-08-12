package com.ticketly.mseventseating.validators;

import com.ticketly.mseventseating.dto.event.SessionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link ValidSalesStartTime} annotation.
 * Validates sales start time rules and other temporal constraints for sessions.
 */
public class SalesStartTimeValidator implements ConstraintValidator<ValidSalesStartTime, SessionRequest> {

    @Override
    public boolean isValid(SessionRequest session, ConstraintValidatorContext context) {
        // Skip validation if essential fields are null - let @NotNull handle these cases
        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
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

        // Skip further validation if sales start rule type is null
        if (session.getSalesStartRuleType() == null) {
            return isValid;
        }

        // Custom validation for each sales start rule type
        switch (session.getSalesStartRuleType()) {
            case IMMEDIATE:
                // For IMMEDIATE sales, we simply ignore salesStartFixedDatetime if it's set
                // No validation errors needed - this is more tolerant of frontend behavior
                break;

            case ROLLING:
                // For ROLLING sales, hours before must be specified and positive
                if (session.getSalesStartHoursBefore() == null) {
                    context.buildConstraintViolationWithTemplate(
                                    "For ROLLING sales, hours before the event must be specified")
                            .addPropertyNode("salesStartHoursBefore")
                            .addConstraintViolation();

                    isValid = false;
                } else if (session.getSalesStartHoursBefore() <= 0) {
                    context.buildConstraintViolationWithTemplate(
                                    "For ROLLING sales, hours before the event must be positive")
                            .addPropertyNode("salesStartHoursBefore")
                            .addConstraintViolation();

                    isValid = false;
                }
                break;

            case FIXED:
                // For FIXED sales, a fixed datetime must be specified
                if (session.getSalesStartFixedDatetime() == null) {
                    context.buildConstraintViolationWithTemplate(
                                    "For FIXED sales, a sales start date and time must be specified")
                            .addPropertyNode("salesStartFixedDatetime")
                            .addConstraintViolation();

                    isValid = false;
                } else if (!session.getSalesStartFixedDatetime().isBefore(session.getStartTime())) {
                    // For FIXED sales, the fixed datetime must be before the session start time
                    context.buildConstraintViolationWithTemplate(
                                    "For FIXED sales, the sales start time must be before the session start time")
                            .addPropertyNode("salesStartFixedDatetime")
                            .addConstraintViolation();

                    isValid = false;
                }
                break;
        }

        return isValid;
    }
}
