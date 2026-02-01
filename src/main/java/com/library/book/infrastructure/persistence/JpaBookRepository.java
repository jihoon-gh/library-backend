package com.library.book.infrastructure.persistence;

import com.library.book.domain.model.Book;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.BookStatus;
import com.library.book.domain.model.Isbn;
import com.library.book.domain.repository.BookRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * BookRepository의 JPA 구현체.
 */
@Repository
@Transactional(readOnly = true)
class JpaBookRepository implements BookRepository {

    private final BookJpaRepository jpaRepository;
    private final BookMapper mapper;

    JpaBookRepository(BookJpaRepository jpaRepository, BookMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public @NonNull Optional<Book> findById(@NonNull BookId id) {
        return jpaRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public @NonNull Optional<Book> findByIsbn(@NonNull Isbn isbn) {
        return jpaRepository.findByIsbn(isbn.normalized())
                .map(mapper::toDomain);
    }

    @Override
    public @NonNull List<Book> findByTitleContaining(@NonNull String title) {
        return jpaRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public @NonNull List<Book> findByAuthorContaining(@NonNull String author) {
        return jpaRepository.findByAuthorContainingIgnoreCase(author)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public @NonNull List<Book> findByStatus(@NonNull BookStatus status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public @NonNull List<Book> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public @NonNull Book save(@NonNull Book book) {
        var entity = mapper.toEntity(book);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void delete(@NonNull BookId id) {
        jpaRepository.deleteById(id.value());
    }

    @Override
    public boolean existsByIsbn(@NonNull Isbn isbn) {
        return jpaRepository.existsByIsbn(isbn.normalized());
    }

    @Override
    public boolean existsById(@NonNull BookId id) {
        return jpaRepository.existsById(id.value());
    }
}
