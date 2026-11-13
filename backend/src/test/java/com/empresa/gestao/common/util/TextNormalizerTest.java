package com.empresa.gestao.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void normalizesCaseAccentsAndSpaces() {
        assertThat(TextNormalizer.normalizeName("  Café   com   Leite  "))
                .isEqualTo("cafe com leite");
        assertThat(TextNormalizer.normalizeName("ARROZ 5 KG"))
                .isEqualTo("arroz 5 kg");
    }

    @Test
    void cleansSkuAndPersonalDocument() {
        assertThat(TextNormalizer.cleanSku("  abc-123 ")).isEqualTo("ABC-123");
        assertThat(TextNormalizer.onlyDigits("529.982.247-25")).isEqualTo("52998224725");
    }

    @Test
    void validCpfIsAcceptedAndInvalidIsRejected() {
        assertThat(CpfValidator.isValid("52998224725")).isTrue();
        assertThat(CpfValidator.isValid("11144477735")).isTrue();
        assertThat(CpfValidator.isValid("11111111111")).isFalse();
        assertThat(CpfValidator.isValid("12345678900")).isFalse();
        assertThat(CpfValidator.isValid(null)).isFalse();
    }
}
