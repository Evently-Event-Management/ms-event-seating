package com.ticketly.mseventseating.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a session has a reasonable duration (not too short or too long).
 */
@Documented
@Constraint(validatedBy = SessionDurationValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSessionDuration {
    String message() default "Invalid session duration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum session duration in minutes
     */
    int minMinutes() default 15;

    /**
     * Maximum session duration in hours
     */
    int maxHours() default 24;
}
