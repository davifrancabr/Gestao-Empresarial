package com.empresa.gestao.common.util;

public final class CpfValidator {

    private CpfValidator() {
    }

    public static boolean isValid(String digits) {
        if (digits == null || !digits.matches("\\d{11}")) {
            return false;
        }
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        int[] numbers = digits.chars().map(c -> c - '0').toArray();
        int first = calculateDigit(numbers, 9);
        int second = calculateDigit(numbers, 10);
        return numbers[9] == first && numbers[10] == second;
    }

    private static int calculateDigit(int[] numbers, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += numbers[i] * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
