package com.ecold.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PlaceholderValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPlaceholders {
    String message() default "Invalid placeholder syntax";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
