package org.tomo.beton.validations;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NotNullEmptyBlankValidatorTest {

    private final NotNullEmptyBlankValidator validator = new NotNullEmptyBlankValidator();

    @ParameterizedTest
    @ValueSource(strings = {"a", " a ", "Vase"})
    void validValues(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n  "})
    void invalidValues(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }
}
