package com.library.book.application.service;

import com.library.book.application.command.CreateBookCommand;
import com.library.book.application.command.UpdateBookCommand;
import com.library.book.application.query.BookSearchCriteria;
import com.library.book.domain.exception.BookNotFoundException;
import com.library.book.domain.exception.DuplicateIsbnException;
import com.library.book.domain.model.Book;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.Isbn;
import com.library.book.domain.repository.BookRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 도서 애플리케이션 서비스.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * 새 도서를 등록합니다.
     */
    @Transactional
    public @NonNull Book createBook(@NonNull CreateBookCommand command) {
        var isbn = Isbn.of(command.isbn());

        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }

        var book = Book.create(
                isbn,
                command.title(),
                command.author(),
                command.publisher(),
                command.publishedDate(),
                command.category()
        );

        return bookRepository.save(book);
    }

    /**
     * 도서 정보를 수정합니다.
     */
    @Transactional
    public @NonNull Book updateBook(
            @NonNull BookId id,
            @NonNull UpdateBookCommand command
    ) {
        var book = findByIdOrThrow(id);

        var updated = book.update(
                command.title(),
                command.author(),
                command.publisher(),
                command.publishedDate(),
                command.category()
        );

        return bookRepository.save(updated);
    }

    /**
     * 도서를 삭제합니다.
     */
    @Transactional
    public void deleteBook(@NonNull BookId id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.delete(id);
    }

    /**
     * ID로 도서를 조회합니다.
     */
    public @NonNull Optional<Book> findById(@NonNull BookId id) {
        return bookRepository.findById(id);
    }

    /**
     * ISBN으로 도서를 조회합니다.
     */
    public @NonNull Optional<Book> findByIsbn(@NonNull String isbn) {
        return bookRepository.findByIsbn(Isbn.of(isbn));
    }

    /**
     * 조건으로 도서를 검색합니다.
     */
    public @NonNull List<Book> search(@NonNull BookSearchCriteria criteria) {
        if (criteria.hasIsbn()) {
            return bookRepository.findByIsbn(Isbn.of(criteria.isbn()))
                    .map(List::of)
                    .orElse(List.of());
        }

        if (criteria.hasTitle()) {
            return bookRepository.findByTitleContaining(criteria.title());
        }

        if (criteria.hasAuthor()) {
            return bookRepository.findByAuthorContaining(criteria.author());
        }

        if (criteria.hasStatus()) {
            return bookRepository.findByStatus(criteria.status());
        }

        return bookRepository.findAll();
    }

    /**
     * 모든 도서를 조회합니다.
     */
    public @NonNull List<Book> findAll() {
        return bookRepository.findAll();
    }

    private Book findByIdOrThrow(BookId id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }
}
