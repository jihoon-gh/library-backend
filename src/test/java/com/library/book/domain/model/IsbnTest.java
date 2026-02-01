package com.library.book.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Isbn")
class IsbnTest {

    @Nested
    @DisplayName("생성자는")
    class Constructor {

        @Test
        @DisplayName("유효한 ISBN-13으로 객체를 생성한다")
        void creates_isbn_with_valid_isbn13() {
            // given
            var validIsbn = "9788966262380";

            // when
            var isbn = Isbn.of(validIsbn);

            // then
            assertThat(isbn.value()).isEqualTo(validIsbn);
        }

        @Test
        @DisplayName("하이픈이 포함된 ISBN-13도 허용한다")
        void accepts_isbn13_with_hyphens() {
            // given
            var isbnWithHyphens = "978-89-6626-238-0";

            // when
            var isbn = Isbn.of(isbnWithHyphens);

            // then
            assertThat(isbn.normalized()).isEqualTo("9788966262380");
        }

        @Test
        @DisplayName("null이면 예외가 발생한다")
        void throws_exception_when_null() {
            // when & then
            assertThatThrownBy(() -> Isbn.of(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("ISBN is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789012", "12345678901234", "invalid", "", "978896626238X"})
        @DisplayName("유효하지 않은 ISBN이면 예외가 발생한다")
        void throws_exception_when_invalid(String invalidIsbn) {
            // when & then
            assertThatThrownBy(() -> Isbn.of(invalidIsbn))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid ISBN-13 format");
        }

        @Test
        @DisplayName("체크섬이 맞지 않으면 예외가 발생한다")
        void throws_exception_when_checksum_invalid() {
            // given - 마지막 체크 디짓이 틀린 ISBN
            var invalidChecksum = "9788966262381";

            // when & then
            assertThatThrownBy(() -> Isbn.of(invalidChecksum))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid ISBN-13 format");
        }
    }

    @Nested
    @DisplayName("normalized 메서드는")
    class Normalized {

        @Test
        @DisplayName("숫자만 포함된 ISBN을 반환한다")
        void returns_digits_only() {
            // given
            var isbn = Isbn.of("978-89-6626-238-0");

            // when
            var normalized = isbn.normalized();

            // then
            assertThat(normalized).isEqualTo("9788966262380");
        }
    }

    @Nested
    @DisplayName("equals 메서드는")
    class Equals {

        @Test
        @DisplayName("같은 ISBN이면 동일하다")
        void same_isbn_are_equal() {
            // given
            var isbn1 = Isbn.of("9788966262380");
            var isbn2 = Isbn.of("9788966262380");

            // then
            assertThat(isbn1).isEqualTo(isbn2);
        }

        @Test
        @DisplayName("다른 ISBN이면 다르다")
        void different_isbn_are_not_equal() {
            // given
            var isbn1 = Isbn.of("9788966262380");
            var isbn2 = Isbn.of("9780134685991");

            // then
            assertThat(isbn1).isNotEqualTo(isbn2);
        }
    }
}
