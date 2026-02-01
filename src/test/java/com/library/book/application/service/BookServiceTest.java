package com.library.book.application.service;

import com.library.book.application.command.CreateBookCommand;
import com.library.book.application.command.UpdateBookCommand;
import com.library.book.application.query.BookSearchCriteria;
import com.library.book.domain.exception.BookNotFoundException;
import com.library.book.domain.exception.DuplicateIsbnException;
import com.library.book.domain.model.Book;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.Category;
import com.library.book.domain.model.Isbn;
import com.library.book.domain.repository.BookRepository;
import com.library.book.fixture.BookFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BookService")
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository);
    }

    @Nested
    @DisplayName("createBook 메서드는")
    class CreateBook {

        @Test
        @DisplayName("유효한 커맨드로 도서를 생성한다")
        void creates_book_with_valid_command() {
            // given
            var command = new CreateBookCommand(
                    BookFixture.VALID_ISBN,
                    "Clean Code",
                    "Robert C. Martin",
                    "인사이트",
                    LocalDate.of(2013, 12, 24),
                    Category.TECHNOLOGY
            );
            when(bookRepository.existsByIsbn(any(Isbn.class))).thenReturn(false);
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            var result = bookService.createBook(command);

            // then
            assertThat(result.title()).isEqualTo(command.title());
            assertThat(result.author()).isEqualTo(command.author());
            verify(bookRepository).save(any(Book.class));
        }

        @Test
        @DisplayName("ISBN이 중복되면 DuplicateIsbnException을 던진다")
        void throws_exception_when_isbn_duplicated() {
            // given
            var command = new CreateBookCommand(
                    BookFixture.VALID_ISBN,
                    "Clean Code",
                    "Robert C. Martin",
                    "인사이트",
                    LocalDate.of(2013, 12, 24),
                    Category.TECHNOLOGY
            );
            when(bookRepository.existsByIsbn(any(Isbn.class))).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> bookService.createBook(command))
                    .isInstanceOf(DuplicateIsbnException.class);

            verify(bookRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateBook 메서드는")
    class UpdateBook {

        @Test
        @DisplayName("도서 정보를 수정한다")
        void updates_book_info() {
            // given
            var book = BookFixture.available();
            var command = new UpdateBookCommand(
                    "Updated Title",
                    "Updated Author",
                    book.publisher(),
                    book.publishedDate(),
                    book.category()
            );
            when(bookRepository.findById(book.id())).thenReturn(Optional.of(book));
            when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            var result = bookService.updateBook(book.id(), command);

            // then
            assertThat(result.title()).isEqualTo("Updated Title");
            assertThat(result.author()).isEqualTo("Updated Author");
        }

        @Test
        @DisplayName("존재하지 않는 도서면 BookNotFoundException을 던진다")
        void throws_exception_when_not_found() {
            // given
            var id = BookId.generate();
            var command = new UpdateBookCommand(
                    "Title", "Author", "Publisher",
                    LocalDate.now(), Category.TECHNOLOGY
            );
            when(bookRepository.findById(id)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> bookService.updateBook(id, command))
                    .isInstanceOf(BookNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteBook 메서드는")
    class DeleteBook {

        @Test
        @DisplayName("도서를 삭제한다")
        void deletes_book() {
            // given
            var id = BookId.generate();
            when(bookRepository.existsById(id)).thenReturn(true);

            // when
            bookService.deleteBook(id);

            // then
            verify(bookRepository).delete(id);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 삭제하면 BookNotFoundException을 던진다")
        void throws_exception_when_book_not_found() {
            // given
            var id = BookId.generate();
            when(bookRepository.existsById(id)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> bookService.deleteBook(id))
                    .isInstanceOf(BookNotFoundException.class);

            verify(bookRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID로 도서를 조회한다")
        void finds_book_by_id() {
            // given
            var book = BookFixture.available();
            when(bookRepository.findById(book.id())).thenReturn(Optional.of(book));

            // when
            var result = bookService.findById(book.id());

            // then
            assertThat(result).contains(book);
        }

        @Test
        @DisplayName("존재하지 않는 ID면 빈 Optional을 반환한다")
        void returns_empty_when_not_found() {
            // given
            var id = BookId.generate();
            when(bookRepository.findById(id)).thenReturn(Optional.empty());

            // when
            var result = bookService.findById(id);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("search 메서드는")
    class Search {

        @Test
        @DisplayName("제목으로 검색하면 일치하는 도서 목록을 반환한다")
        void returns_books_matching_title() {
            // given
            var books = BookFixture.listOf(3);
            when(bookRepository.findByTitleContaining("Clean"))
                    .thenReturn(books);

            // when
            var result = bookService.search(BookSearchCriteria.byTitle("Clean"));

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("저자로 검색하면 일치하는 도서 목록을 반환한다")
        void returns_books_matching_author() {
            // given
            var books = BookFixture.listOf(2);
            when(bookRepository.findByAuthorContaining("Martin"))
                    .thenReturn(books);

            // when
            var result = bookService.search(BookSearchCriteria.byAuthor("Martin"));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("ISBN으로 검색하면 일치하는 도서를 반환한다")
        void returns_book_matching_isbn() {
            // given
            var book = BookFixture.available();
            when(bookRepository.findByIsbn(any(Isbn.class)))
                    .thenReturn(Optional.of(book));

            // when
            var result = bookService.search(BookSearchCriteria.byIsbn(BookFixture.VALID_ISBN));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst()).isEqualTo(book);
        }

        @Test
        @DisplayName("조건이 없으면 전체 도서 목록을 반환한다")
        void returns_all_books_when_no_criteria() {
            // given
            var books = BookFixture.listOf(5);
            when(bookRepository.findAll()).thenReturn(books);

            // when
            var result = bookService.search(BookSearchCriteria.empty());

            // then
            assertThat(result).hasSize(5);
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    class FindAll {

        @Test
        @DisplayName("모든 도서를 반환한다")
        void returns_all_books() {
            // given
            var books = BookFixture.listOf(10);
            when(bookRepository.findAll()).thenReturn(books);

            // when
            var result = bookService.findAll();

            // then
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("도서가 없으면 빈 리스트를 반환한다")
        void returns_empty_list_when_no_books() {
            // given
            when(bookRepository.findAll()).thenReturn(List.of());

            // when
            var result = bookService.findAll();

            // then
            assertThat(result).isEmpty();
        }
    }
}
