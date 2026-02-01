package com.library.book.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository.
 */
interface BookJpaRepository extends JpaRepository<BookJpaEntity, UUID> {

    Optional<BookJpaEntity> findByIsbn(String isbn);

    List<BookJpaEntity> findByTitleContainingIgnoreCase(String title);

    List<BookJpaEntity> findByAuthorContainingIgnoreCase(String author);

    List<BookJpaEntity> findByStatus(String status);

    boolean existsByIsbn(String isbn);
}
