package com.library.book.domain.exception;

/**
 * Book 도메인의 기본 예외 (sealed class).
 */
public sealed class BookException extends RuntimeException
        permits BookNotFoundException, DuplicateIsbnException, InvalidBookStateException {

    public BookException(String message) {
        super(message);
    }

    public BookException(String message, Throwable cause) {
        super(message, cause);
    }
}
