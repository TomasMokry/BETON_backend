package org.tomo.beton.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotNullEmptyBlankValidator implements ConstraintValidator<NotNullEmptyBlank, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return !value.isBlank();
    }
}
