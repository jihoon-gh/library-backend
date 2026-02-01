package com.library.book.presentation.rest.request;

import com.library.book.application.query.BookSearchCriteria;
import com.library.book.domain.model.BookStatus;
import org.jspecify.annotations.Nullable;

/**
 * 도서 검색 요청 DTO.
 */
public record SearchBookRequest(
        @Nullable String title,
        @Nullable String author,
        @Nullable String isbn,
        @Nullable BookStatus status
) {
    public BookSearchCriteria toCriteria() {
        return new BookSearchCriteria(title, author, isbn, status);
    }
}
