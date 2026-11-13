package com.empresa.gestao.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * Normaliza nomes para comparação: remove acentos, colapsa espaços,
     * aplica trim e converte para minúsculas.
     */
    public static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    public static String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    public static String cleanSku(String value) {
        String cleaned = cleanText(value);
        return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    public static String onlyDigits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
