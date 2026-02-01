package com.library.book.fixture;

import com.library.book.domain.model.Book;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.BookStatus;
import com.library.book.domain.model.Category;
import com.library.book.domain.model.Isbn;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Book 도메인 테스트를 위한 픽스처.
 */
public final class BookFixture {

    public static final String VALID_ISBN = "9788966262380";

    private BookFixture() {
    }

    public static Book available() {
        return new Book(
                BookId.generate(),
                Isbn.of(VALID_ISBN),
                "Clean Code",
                "Robert C. Martin",
                "인사이트",
                LocalDate.of(2013, 12, 24),
                Category.TECHNOLOGY,
                BookStatus.AVAILABLE,
                Instant.now(),
                Instant.now()
        );
    }

    public static Book borrowed() {
        return available().borrow();
    }

    public static Book withTitle(String title) {
        return new Book(
                BookId.generate(),
                Isbn.of(VALID_ISBN),
                title,
                "Test Author",
                "Test Publisher",
                LocalDate.of(2020, 1, 1),
                Category.TECHNOLOGY,
                BookStatus.AVAILABLE,
                Instant.now(),
                Instant.now()
        );
    }

    public static Book withIsbn(String isbn) {
        return new Book(
                BookId.generate(),
                Isbn.of(isbn),
                "Test Book",
                "Test Author",
                "Test Publisher",
                LocalDate.of(2020, 1, 1),
                Category.TECHNOLOGY,
                BookStatus.AVAILABLE,
                Instant.now(),
                Instant.now()
        );
    }

    public static Book withId(BookId id) {
        return new Book(
                id,
                Isbn.of(VALID_ISBN),
                "Test Book",
                "Test Author",
                "Test Publisher",
                LocalDate.of(2020, 1, 1),
                Category.TECHNOLOGY,
                BookStatus.AVAILABLE,
                Instant.now(),
                Instant.now()
        );
    }

    public static List<Book> listOf(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> withTitle("Book " + i))
                .toList();
    }
}
