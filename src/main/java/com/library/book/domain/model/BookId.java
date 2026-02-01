package com.library.book.domain.model;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Book 애그리거트의 식별자.
 * UUID 기반으로 전역 고유성을 보장합니다.
 */
public record BookId(@NonNull UUID value) {

    public BookId {
        Objects.requireNonNull(value, "BookId value is required");
    }

    public static BookId generate() {
        return new BookId(UUID.randomUUID());
    }

    public static BookId of(@NonNull UUID value) {
        return new BookId(value);
    }

    public static BookId of(@NonNull String value) {
        Objects.requireNonNull(value, "BookId string value is required");
        return new BookId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
