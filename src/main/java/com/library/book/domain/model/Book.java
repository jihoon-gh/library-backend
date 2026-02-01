package com.library.book.domain.model;

import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 도서 애그리거트 루트.
 *
 * 불변 객체로 설계되어 모든 상태 변경은 새 객체를 반환합니다.
 */
public record Book(
        @NonNull BookId id,
        @NonNull Isbn isbn,
        @NonNull String title,
        @NonNull String author,
        @NonNull String publisher,
        @NonNull LocalDate publishedDate,
        @NonNull Category category,
        @NonNull BookStatus status,
        @NonNull Instant createdAt,
        @NonNull Instant updatedAt
) {

    public Book {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(isbn, "isbn is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(author, "author is required");
        Objects.requireNonNull(publisher, "publisher is required");
        Objects.requireNonNull(publishedDate, "publishedDate is required");
        Objects.requireNonNull(category, "category is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");

        if (title.isBlank()) {
            throw new IllegalArgumentException("title cannot be blank");
        }
        if (author.isBlank()) {
            throw new IllegalArgumentException("author cannot be blank");
        }
        if (publisher.isBlank()) {
            throw new IllegalArgumentException("publisher cannot be blank");
        }
    }

    /**
     * 새 도서를 생성합니다.
     */
    public static Book create(
            @NonNull Isbn isbn,
            @NonNull String title,
            @NonNull String author,
            @NonNull String publisher,
            @NonNull LocalDate publishedDate,
            @NonNull Category category
    ) {
        var now = Instant.now();
        return new Book(
                BookId.generate(),
                isbn,
                title,
                author,
                publisher,
                publishedDate,
                category,
                BookStatus.AVAILABLE,
                now,
                now
        );
    }

    /**
     * 도서 정보를 수정합니다.
     */
    public Book update(
            @NonNull String title,
            @NonNull String author,
            @NonNull String publisher,
            @NonNull LocalDate publishedDate,
            @NonNull Category category
    ) {
        return new Book(
                this.id,
                this.isbn,
                title,
                author,
                publisher,
                publishedDate,
                category,
                this.status,
                this.createdAt,
                Instant.now()
        );
    }

    /**
     * 도서를 대출합니다.
     */
    public Book borrow() {
        if (!status.canBorrow()) {
            throw new IllegalStateException("Cannot borrow book in status: " + status);
        }
        return new Book(
                id, isbn, title, author, publisher, publishedDate,
                category, BookStatus.BORROWED, createdAt, Instant.now()
        );
    }

    /**
     * 도서를 반납합니다.
     */
    public Book returnBook() {
        if (!status.canReturn()) {
            throw new IllegalStateException("Cannot return book in status: " + status);
        }
        return new Book(
                id, isbn, title, author, publisher, publishedDate,
                category, BookStatus.AVAILABLE, createdAt, Instant.now()
        );
    }

    public boolean isAvailable() {
        return status == BookStatus.AVAILABLE;
    }

    public boolean isBorrowed() {
        return status == BookStatus.BORROWED;
    }
}
