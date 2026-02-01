package com.library.book.domain.exception;

import com.library.book.domain.model.Isbn;

public final class DuplicateIsbnException extends BookException {

    public DuplicateIsbnException(Isbn isbn) {
        super("Book with ISBN already exists: " + isbn.value());
    }
}
