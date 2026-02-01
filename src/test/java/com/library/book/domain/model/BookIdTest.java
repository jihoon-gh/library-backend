package com.library.book.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BookId")
class BookIdTest {

    @Nested
    @DisplayName("generate 메서드는")
    class Generate {

        @Test
        @DisplayName("고유한 BookId를 생성한다")
        void generates_unique_book_id() {
            // when
            var id1 = BookId.generate();
            var id2 = BookId.generate();

            // then
            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("of(UUID) 메서드는")
    class OfUuid {

        @Test
        @DisplayName("주어진 UUID로 BookId를 생성한다")
        void creates_book_id_from_uuid() {
            // given
            var uuid = UUID.randomUUID();

            // when
            var bookId = BookId.of(uuid);

            // then
            assertThat(bookId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("null UUID이면 예외가 발생한다")
        void throws_exception_when_uuid_is_null() {
            // when & then
            assertThatThrownBy(() -> BookId.of((UUID) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("BookId value is required");
        }
    }

    @Nested
    @DisplayName("of(String) 메서드는")
    class OfString {

        @Test
        @DisplayName("문자열 UUID로 BookId를 생성한다")
        void creates_book_id_from_string() {
            // given
            var uuid = UUID.randomUUID();
            var uuidString = uuid.toString();

            // when
            var bookId = BookId.of(uuidString);

            // then
            assertThat(bookId.value()).isEqualTo(uuid);
        }

        @Test
        @DisplayName("잘못된 형식의 문자열이면 예외가 발생한다")
        void throws_exception_when_string_is_invalid() {
            // when & then
            assertThatThrownBy(() -> BookId.of("invalid-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("toString 메서드는")
    class ToString {

        @Test
        @DisplayName("UUID 문자열을 반환한다")
        void returns_uuid_string() {
            // given
            var uuid = UUID.randomUUID();
            var bookId = BookId.of(uuid);

            // when
            var result = bookId.toString();

            // then
            assertThat(result).isEqualTo(uuid.toString());
        }
    }
}
