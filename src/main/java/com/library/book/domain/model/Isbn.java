package com.library.book.domain.model;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ISBN (International Standard Book Number) 값 객체.
 * ISBN-13 형식을 지원합니다.
 */
public record Isbn(@NonNull String value) {

    private static final Pattern DIGITS_ONLY = Pattern.compile("[^0-9]");

    public Isbn {
        Objects.requireNonNull(value, "ISBN is required");
        var normalized = normalize(value);
        if (!isValidIsbn13(normalized)) {
            throw new IllegalArgumentException("Invalid ISBN-13 format: " + value);
        }
    }

    public static Isbn of(@NonNull String value) {
        return new Isbn(value);
    }

    /**
     * 정규화된 ISBN (숫자만)을 반환합니다.
     */
    public String normalized() {
        return normalize(value);
    }

    private static String normalize(String isbn) {
        return DIGITS_ONLY.matcher(isbn).replaceAll("");
    }

    private static boolean isValidIsbn13(String isbn) {
        if (isbn.length() != 13) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(isbn.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == Character.getNumericValue(isbn.charAt(12));
    }
}
