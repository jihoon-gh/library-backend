package com.library.book.presentation.rest.response;

import com.library.book.domain.model.Book;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 도서 목록 응답 DTO.
 */
public record BookListResponse(
        @NonNull List<BookResponse> books,
        int totalCount
) {
    public static BookListResponse from(@NonNull List<Book> books) {
        var responses = books.stream()
                .map(BookResponse::from)
                .toList();
        return new BookListResponse(responses, responses.size());
    }
}
