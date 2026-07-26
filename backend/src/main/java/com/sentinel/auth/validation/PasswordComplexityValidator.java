package com.sentinel.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordComplexityValidator implements ConstraintValidator<ValidPassword, String> {

    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String DIGIT_PATTERN = ".*\\d.*";
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return true;
        return password.matches(UPPERCASE_PATTERN)
                && password.matches(DIGIT_PATTERN)
                && password.matches(SPECIAL_CHAR_PATTERN);
    }
}
