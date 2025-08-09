package com.ticketly.mseventseating.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = SessionLocationValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSessionLocation {
    String message() default "Invalid location details for the selected session type.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
