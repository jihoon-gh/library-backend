package com.library.book.presentation.rest.request;

import com.library.book.application.command.CreateBookCommand;
import com.library.book.domain.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 도서 생성 요청 DTO.
 */
public record CreateBookRequest(
        @NotBlank(message = "ISBN은 필수입니다")
        @Pattern(regexp = "^[0-9]{13}$", message = "ISBN은 13자리 숫자여야 합니다")
        String isbn,

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 500, message = "제목은 500자 이하여야 합니다")
        String title,

        @NotBlank(message = "저자는 필수입니다")
        @Size(max = 200, message = "저자명은 200자 이하여야 합니다")
        String author,

        @NotBlank(message = "출판사는 필수입니다")
        @Size(max = 200, message = "출판사명은 200자 이하여야 합니다")
        String publisher,

        @NotNull(message = "출판일은 필수입니다")
        @PastOrPresent(message = "출판일은 과거 또는 현재여야 합니다")
        LocalDate publishedDate,

        @NotNull(message = "카테고리는 필수입니다")
        Category category
) {
    public CreateBookCommand toCommand() {
        return new CreateBookCommand(
                isbn, title, author, publisher, publishedDate, category
        );
    }
}
