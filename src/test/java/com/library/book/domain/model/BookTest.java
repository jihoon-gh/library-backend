package com.library.book.domain.model;

import com.library.book.fixture.BookFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Book")
class BookTest {

    @Nested
    @DisplayName("create 메서드는")
    class Create {

        @Test
        @DisplayName("유효한 정보로 새 도서를 생성한다")
        void creates_new_book_with_valid_info() {
            // given
            var isbn = Isbn.of("9788966262380");
            var title = "Clean Code";
            var author = "Robert C. Martin";
            var publisher = "인사이트";
            var publishedDate = LocalDate.of(2013, 12, 24);
            var category = Category.TECHNOLOGY;

            // when
            var book = Book.create(isbn, title, author, publisher, publishedDate, category);

            // then
            assertThat(book.id()).isNotNull();
            assertThat(book.isbn()).isEqualTo(isbn);
            assertThat(book.title()).isEqualTo(title);
            assertThat(book.author()).isEqualTo(author);
            assertThat(book.publisher()).isEqualTo(publisher);
            assertThat(book.publishedDate()).isEqualTo(publishedDate);
            assertThat(book.category()).isEqualTo(category);
            assertThat(book.status()).isEqualTo(BookStatus.AVAILABLE);
            assertThat(book.createdAt()).isNotNull();
            assertThat(book.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("빈 제목으로 생성하면 예외가 발생한다")
        void throws_exception_when_title_is_blank() {
            // given
            var isbn = Isbn.of("9788966262380");

            // when & then
            assertThatThrownBy(() -> Book.create(
                    isbn, "  ", "Author", "Publisher",
                    LocalDate.now(), Category.TECHNOLOGY
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("title cannot be blank");
        }

        @Test
        @DisplayName("빈 저자로 생성하면 예외가 발생한다")
        void throws_exception_when_author_is_blank() {
            // given
            var isbn = Isbn.of("9788966262380");

            // when & then
            assertThatThrownBy(() -> Book.create(
                    isbn, "Title", "  ", "Publisher",
                    LocalDate.now(), Category.TECHNOLOGY
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("author cannot be blank");
        }

        @Test
        @DisplayName("빈 출판사로 생성하면 예외가 발생한다")
        void throws_exception_when_publisher_is_blank() {
            // given
            var isbn = Isbn.of("9788966262380");

            // when & then
            assertThatThrownBy(() -> Book.create(
                    isbn, "Title", "Author", "  ",
                    LocalDate.now(), Category.TECHNOLOGY
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("publisher cannot be blank");
        }
    }

    @Nested
    @DisplayName("update 메서드는")
    class Update {

        @Test
        @DisplayName("도서 정보를 수정한 새 객체를 반환한다")
        void returns_updated_book() {
            // given
            var book = BookFixture.available();
            var newTitle = "Updated Title";
            var newAuthor = "New Author";

            // when
            var updated = book.update(
                    newTitle, newAuthor, book.publisher(),
                    book.publishedDate(), book.category()
            );

            // then
            assertThat(updated.id()).isEqualTo(book.id());
            assertThat(updated.isbn()).isEqualTo(book.isbn());
            assertThat(updated.title()).isEqualTo(newTitle);
            assertThat(updated.author()).isEqualTo(newAuthor);
            assertThat(updated.status()).isEqualTo(book.status());
            assertThat(updated.createdAt()).isEqualTo(book.createdAt());
            assertThat(updated.updatedAt()).isAfterOrEqualTo(book.updatedAt());
        }

        @Test
        @DisplayName("원본 도서는 변경되지 않는다")
        void does_not_modify_original_book() {
            // given
            var book = BookFixture.available();
            var originalTitle = book.title();

            // when
            book.update("New Title", book.author(), book.publisher(),
                    book.publishedDate(), book.category());

            // then
            assertThat(book.title()).isEqualTo(originalTitle);
        }
    }

    @Nested
    @DisplayName("borrow 메서드는")
    class Borrow {

        @Test
        @DisplayName("대출 가능한 도서를 대출 상태로 변경한다")
        void changes_available_book_to_borrowed() {
            // given
            var book = BookFixture.available();

            // when
            var borrowed = book.borrow();

            // then
            assertThat(borrowed.status()).isEqualTo(BookStatus.BORROWED);
            assertThat(borrowed.isBorrowed()).isTrue();
            assertThat(borrowed.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("이미 대출 중인 도서를 대출하면 예외가 발생한다")
        void throws_exception_when_already_borrowed() {
            // given
            var book = BookFixture.borrowed();

            // when & then
            assertThatThrownBy(book::borrow)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot borrow book in status");
        }
    }

    @Nested
    @DisplayName("returnBook 메서드는")
    class ReturnBook {

        @Test
        @DisplayName("대출 중인 도서를 대출 가능 상태로 변경한다")
        void changes_borrowed_book_to_available() {
            // given
            var book = BookFixture.borrowed();

            // when
            var returned = book.returnBook();

            // then
            assertThat(returned.status()).isEqualTo(BookStatus.AVAILABLE);
            assertThat(returned.isAvailable()).isTrue();
            assertThat(returned.isBorrowed()).isFalse();
        }

        @Test
        @DisplayName("대출 가능한 도서를 반납하면 예외가 발생한다")
        void throws_exception_when_already_available() {
            // given
            var book = BookFixture.available();

            // when & then
            assertThatThrownBy(book::returnBook)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot return book in status");
        }
    }

    @Nested
    @DisplayName("isAvailable 메서드는")
    class IsAvailable {

        @Test
        @DisplayName("대출 가능 상태면 true를 반환한다")
        void returns_true_when_available() {
            // given
            var book = BookFixture.available();

            // when & then
            assertThat(book.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("대출 중이면 false를 반환한다")
        void returns_false_when_borrowed() {
            // given
            var book = BookFixture.borrowed();

            // when & then
            assertThat(book.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isBorrowed 메서드는")
    class IsBorrowed {

        @Test
        @DisplayName("대출 중이면 true를 반환한다")
        void returns_true_when_borrowed() {
            // given
            var book = BookFixture.borrowed();

            // when & then
            assertThat(book.isBorrowed()).isTrue();
        }

        @Test
        @DisplayName("대출 가능 상태면 false를 반환한다")
        void returns_false_when_available() {
            // given
            var book = BookFixture.available();

            // when & then
            assertThat(book.isBorrowed()).isFalse();
        }
    }
}
