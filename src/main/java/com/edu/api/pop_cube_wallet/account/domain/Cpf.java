package com.edu.api.pop_cube_wallet.account.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * CPF (Cadastro de Pessoas Físicas) value object with validation.
 * Immutable; invalid values are rejected at construction time.
 */
@ValueObject
public record Cpf(String value) {

    public Cpf {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CPF must not be blank");
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11) {
            throw new IllegalArgumentException("CPF must contain exactly 11 digits");
        }
        if (digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CPF must not consist of identical digits");
        }
        if (!isChecksumValid(digits)) {
            throw new IllegalArgumentException("CPF checksum is invalid");
        }
    }

    /**
     * Returns the raw 11-digit string (no punctuation).
     */
    public String digits() {
        return value.replaceAll("\\D", "");
    }

    private static boolean isChecksumValid(String digits) {
        int[] weights1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

        int sum1 = 0;
        for (int i = 0; i < 9; i++) {
            sum1 += Character.getNumericValue(digits.charAt(i)) * weights1[i];
        }
        int check1 = 11 - (sum1 % 11);
        if (check1 >= 10) check1 = 0;
        if (check1 != Character.getNumericValue(digits.charAt(9))) return false;

        int sum2 = 0;
        for (int i = 0; i < 10; i++) {
            sum2 += Character.getNumericValue(digits.charAt(i)) * weights2[i];
        }
        int check2 = 11 - (sum2 % 11);
        if (check2 >= 10) check2 = 0;
        return check2 == Character.getNumericValue(digits.charAt(10));
    }
}
