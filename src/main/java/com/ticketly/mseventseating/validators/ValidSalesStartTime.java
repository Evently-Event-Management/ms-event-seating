package com.ticketly.mseventseating.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that the sales start time is valid for the given sales rule type.
 * For example, for IMMEDIATE sales, the sales start time must not be before the session start time.
 * 
 * This annotation can be applied to both SessionRequest and SessionTimeUpdateDTO classes.
 */
@Documented
@Constraint(validatedBy = {SalesStartTimeValidator.class, SessionTimeUpdateValidator.class})
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSalesStartTime {
    String message() default "Invalid sales start time configuration";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
