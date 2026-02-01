package com.library.book.presentation.rest.response;

import com.library.book.domain.model.Book;
import com.library.book.domain.model.BookStatus;
import com.library.book.domain.model.Category;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 도서 응답 DTO.
 */
public record BookResponse(
        @NonNull String id,
        @NonNull String isbn,
        @NonNull String title,
        @NonNull String author,
        @NonNull String publisher,
        @NonNull LocalDate publishedDate,
        @NonNull Category category,
        @NonNull BookStatus status,
        @NonNull Instant createdAt,
        @NonNull Instant updatedAt
) {
    public static BookResponse from(@NonNull Book book) {
        return new BookResponse(
                book.id().toString(),
                book.isbn().value(),
                book.title(),
                book.author(),
                book.publisher(),
                book.publishedDate(),
                book.category(),
                book.status(),
                book.createdAt(),
                book.updatedAt()
        );
    }
}
