package com.rishivyas.rentalscam.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactNormalizerTest {

    @Test
    void irishNationalAndInternationalFormatsMatch() {
        assertThat(ContactNormalizer.phone("087 123 4567")).isEqualTo("+353871234567");
        assertThat(ContactNormalizer.phone("+353 (87) 123-4567")).isEqualTo("+353871234567");
        assertThat(ContactNormalizer.phone("00353871234567")).isEqualTo("+353871234567");
        assertThat(ContactNormalizer.isIrishNumber("+353871234567")).isTrue();
        assertThat(ContactNormalizer.isIrishNumber("+447700900123")).isFalse();
    }

    @Test
    void gmailDotsAndTagsAreIgnored() {
        assertThat(ContactNormalizer.email("John.Smith+rent@GMail.com")).isEqualTo("johnsmith@gmail.com");
        assertThat(ContactNormalizer.email("john.smith@example.ie")).isEqualTo("john.smith@example.ie");
    }

    @Test
    void blankValuesNormalizeToNull() {
        assertThat(ContactNormalizer.phone("  ")).isNull();
        assertThat(ContactNormalizer.email(null)).isNull();
    }
}
