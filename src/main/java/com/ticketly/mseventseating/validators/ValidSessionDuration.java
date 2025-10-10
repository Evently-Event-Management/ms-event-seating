package com.ticketly.mseventseating.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that session duration is within acceptable limits.
 * This annotation can be applied to both SessionRequest and SessionTimeUpdateDTO classes.
 */
@Documented
@Constraint(validatedBy = {SessionDurationValidator.class, SessionTimeUpdateDurationValidator.class})
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
