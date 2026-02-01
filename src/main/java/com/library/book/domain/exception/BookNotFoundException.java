package com.library.book.domain.exception;

import com.library.book.domain.model.BookId;
import com.library.book.domain.model.Isbn;

public final class BookNotFoundException extends BookException {

    public BookNotFoundException(BookId id) {
        super("Book not found with id: " + id);
    }

    public BookNotFoundException(Isbn isbn) {
        super("Book not found with ISBN: " + isbn.value());
    }
}
