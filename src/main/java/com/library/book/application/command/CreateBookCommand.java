package com.library.book.application.command;

import com.library.book.domain.model.Category;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 도서 생성 커맨드.
 */
public record CreateBookCommand(
        @NonNull String isbn,
        @NonNull String title,
        @NonNull String author,
        @NonNull String publisher,
        @NonNull LocalDate publishedDate,
        @NonNull Category category
) {
    public CreateBookCommand {
        Objects.requireNonNull(isbn, "isbn is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(author, "author is required");
        Objects.requireNonNull(publisher, "publisher is required");
        Objects.requireNonNull(publishedDate, "publishedDate is required");
        Objects.requireNonNull(category, "category is required");
    }
}
