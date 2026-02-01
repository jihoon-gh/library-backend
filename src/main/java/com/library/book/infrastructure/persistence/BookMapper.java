package com.library.book.infrastructure.persistence;

import com.library.book.domain.model.Book;
import com.library.book.domain.model.BookId;
import com.library.book.domain.model.BookStatus;
import com.library.book.domain.model.Category;
import com.library.book.domain.model.Isbn;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * Book 도메인 모델과 JPA 엔티티 간 변환.
 */
@Component
class BookMapper {

    @NonNull
    Book toDomain(@NonNull BookJpaEntity entity) {
        return new Book(
                BookId.of(entity.getId()),
                Isbn.of(entity.getIsbn()),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getPublisher(),
                entity.getPublishedDate(),
                Category.valueOf(entity.getCategory()),
                BookStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @NonNull
    BookJpaEntity toEntity(@NonNull Book book) {
        var entity = new BookJpaEntity();
        entity.setId(book.id().value());
        entity.setIsbn(book.isbn().normalized());
        entity.setTitle(book.title());
        entity.setAuthor(book.author());
        entity.setPublisher(book.publisher());
        entity.setPublishedDate(book.publishedDate());
        entity.setCategory(book.category().name());
        entity.setStatus(book.status().name());
        entity.setCreatedAt(book.createdAt());
        entity.setUpdatedAt(book.updatedAt());
        return entity;
    }
}
