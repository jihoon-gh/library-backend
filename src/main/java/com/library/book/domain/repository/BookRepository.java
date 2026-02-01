package com.library.book.domain.repository;

import com.library.book.domain.model.Book;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.BookStatus;
import com.library.book.domain.model.Isbn;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Book 애그리거트의 저장소 인터페이스.
 *
 * 도메인 계층에 위치하며, 구현체는 인프라스트럭처 계층에 있습니다.
 */
public interface BookRepository {

    @NonNull
    Optional<Book> findById(@NonNull BookId id);

    @NonNull
    Optional<Book> findByIsbn(@NonNull Isbn isbn);

    @NonNull
    List<Book> findByTitleContaining(@NonNull String title);

    @NonNull
    List<Book> findByAuthorContaining(@NonNull String author);

    @NonNull
    List<Book> findByStatus(@NonNull BookStatus status);

    @NonNull
    List<Book> findAll();

    @NonNull
    Book save(@NonNull Book book);

    void delete(@NonNull BookId id);

    boolean existsByIsbn(@NonNull Isbn isbn);

    boolean existsById(@NonNull BookId id);
}
