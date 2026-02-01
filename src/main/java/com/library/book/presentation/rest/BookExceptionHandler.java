package com.library.book.presentation.rest;

import com.library.book.domain.exception.BookNotFoundException;
import com.library.book.domain.exception.DuplicateIsbnException;
import com.library.book.domain.exception.InvalidBookStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Book 도메인 예외 처리기.
 */
@RestControllerAdvice(assignableTypes = BookController.class)
class BookExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    ProblemDetail handleBookNotFound(BookNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage()
        );
        problem.setTitle("Book Not Found");
        problem.setType(URI.create("/errors/book-not-found"));
        return problem;
    }

    @ExceptionHandler(DuplicateIsbnException.class)
    ProblemDetail handleDuplicateIsbn(DuplicateIsbnException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage()
        );
        problem.setTitle("Duplicate ISBN");
        problem.setType(URI.create("/errors/duplicate-isbn"));
        return problem;
    }

    @ExceptionHandler(InvalidBookStateException.class)
    ProblemDetail handleInvalidState(InvalidBookStateException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage()
        );
        problem.setTitle("Invalid Book State");
        problem.setType(URI.create("/errors/invalid-book-state"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage()
        );
        problem.setTitle("Bad Request");
        problem.setType(URI.create("/errors/bad-request"));
        return problem;
    }
}
