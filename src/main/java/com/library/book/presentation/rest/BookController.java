package com.library.book.presentation.rest;

import com.library.book.application.service.BookService;
import com.library.book.domain.exception.BookNotFoundException;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.Isbn;
import com.library.book.presentation.rest.request.CreateBookRequest;
import com.library.book.presentation.rest.request.SearchBookRequest;
import com.library.book.presentation.rest.request.UpdateBookRequest;
import com.library.book.presentation.rest.response.BookListResponse;
import com.library.book.presentation.rest.response.BookResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * 도서 REST 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * POST /api/v1/books - 새 도서 등록
     */
    @PostMapping
    public ResponseEntity<BookResponse> createBook(
            @Valid @RequestBody CreateBookRequest request
    ) {
        var book = bookService.createBook(request.toCommand());
        var response = BookResponse.from(book);
        var location = URI.create("/api/v1/books/" + book.id());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * GET /api/v1/books/{id} - ID로 도서 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBook(@PathVariable UUID id) {
        var book = bookService.findById(BookId.of(id))
                .orElseThrow(() -> new BookNotFoundException(BookId.of(id)));
        return ResponseEntity.ok(BookResponse.from(book));
    }

    /**
     * GET /api/v1/books - 도서 목록 조회 및 검색
     */
    @GetMapping
    public ResponseEntity<BookListResponse> getBooks(
            @ModelAttribute SearchBookRequest request
    ) {
        var books = bookService.search(request.toCriteria());
        return ResponseEntity.ok(BookListResponse.from(books));
    }

    /**
     * PUT /api/v1/books/{id} - 도서 정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBookRequest request
    ) {
        var book = bookService.updateBook(BookId.of(id), request.toCommand());
        return ResponseEntity.ok(BookResponse.from(book));
    }

    /**
     * DELETE /api/v1/books/{id} - 도서 삭제
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(BookId.of(id));
    }

    /**
     * GET /api/v1/books/isbn/{isbn} - ISBN으로 도서 조회
     */
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookResponse> getBookByIsbn(@PathVariable String isbn) {
        var book = bookService.findByIsbn(isbn)
                .orElseThrow(() -> new BookNotFoundException(Isbn.of(isbn)));
        return ResponseEntity.ok(BookResponse.from(book));
    }
}
