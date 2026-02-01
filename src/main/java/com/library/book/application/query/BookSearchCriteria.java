package com.library.book.application.query;

import com.library.book.domain.model.BookStatus;
import org.jspecify.annotations.Nullable;

/**
 * 도서 검색 조건.
 */
public record BookSearchCriteria(
        @Nullable String title,
        @Nullable String author,
        @Nullable String isbn,
        @Nullable BookStatus status
) {
    public static BookSearchCriteria byTitle(String title) {
        return new BookSearchCriteria(title, null, null, null);
    }

    public static BookSearchCriteria byAuthor(String author) {
        return new BookSearchCriteria(null, author, null, null);
    }

    public static BookSearchCriteria byIsbn(String isbn) {
        return new BookSearchCriteria(null, null, isbn, null);
    }

    public static BookSearchCriteria byStatus(BookStatus status) {
        return new BookSearchCriteria(null, null, null, status);
    }

    public static BookSearchCriteria empty() {
        return new BookSearchCriteria(null, null, null, null);
    }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public boolean hasAuthor() {
        return author != null && !author.isBlank();
    }

    public boolean hasIsbn() {
        return isbn != null && !isbn.isBlank();
    }

    public boolean hasStatus() {
        return status != null;
    }
}
