package org.tomo.beton.validations;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LowercaseValidatorTest {

    private final LowercaseValidator validator = new LowercaseValidator();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "tom@mail.com", "123", "abc-def"})
    void validValues(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Tom@mail.com", "TOM", "tom@Mail.com"})
    void invalidValues(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }
}
