package com.ticketly.mseventseating.validators;

import com.ticketly.mseventseating.dto.session.SessionTimeUpdateDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Duration;

/**
 * Validator implementation for the {@link ValidSessionDuration} annotation when used with SessionTimeUpdateDTO.
 * Ensures that sessions have a reasonable duration - not too short or too long.
 */
public class SessionTimeUpdateDurationValidator implements ConstraintValidator<ValidSessionDuration, SessionTimeUpdateDTO> {
    
    private int minMinutes;
    private int maxHours;
    
    @Override
    public void initialize(ValidSessionDuration constraintAnnotation) {
        this.minMinutes = constraintAnnotation.minMinutes();
        this.maxHours = constraintAnnotation.maxHours();
    }
    
    @Override
    public boolean isValid(SessionTimeUpdateDTO session, ConstraintValidatorContext context) {
        // Skip validation if essential fields are null
        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
            return true; // Let @NotNull annotations handle these cases
        }
        
        // Calculate duration in minutes
        Duration duration = Duration.between(session.getStartTime(), session.getEndTime());
        long durationMinutes = duration.toMinutes();
        
        // Disable default constraint violation to use custom error messages
        context.disableDefaultConstraintViolation();
        
        // Check if the session is too short
        if (durationMinutes < minMinutes) {
            context.buildConstraintViolationWithTemplate(
                    "Session duration must be at least " + minMinutes + " minutes")
                    .addConstraintViolation();
            return false;
        }
        
        // Check if the session is too long
        if (durationMinutes > (maxHours * 60L)) {
            context.buildConstraintViolationWithTemplate(
                    "Session duration cannot exceed " + maxHours + " hours")
                    .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}