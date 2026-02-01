package com.library.book.domain.exception;

import com.library.book.domain.model.BookStatus;

public final class InvalidBookStateException extends BookException {

    public InvalidBookStateException(String operation, BookStatus currentStatus) {
        super("Cannot %s book in status: %s".formatted(operation, currentStatus));
    }
}
