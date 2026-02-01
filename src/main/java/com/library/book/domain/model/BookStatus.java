package com.library.book.domain.model;

/**
 * 도서의 대출 상태를 나타냅니다.
 */
public enum BookStatus {

    AVAILABLE("대출 가능"),
    BORROWED("대출 중");

    private final String description;

    BookStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public boolean canBorrow() {
        return this == AVAILABLE;
    }

    public boolean canReturn() {
        return this == BORROWED;
    }
}
