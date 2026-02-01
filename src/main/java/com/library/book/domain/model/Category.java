package com.library.book.domain.model;

/**
 * 도서 카테고리.
 */
public enum Category {

    FICTION("문학"),
    NON_FICTION("비문학"),
    SCIENCE("과학"),
    TECHNOLOGY("기술"),
    HISTORY("역사"),
    PHILOSOPHY("철학"),
    ART("예술"),
    CHILDREN("아동"),
    OTHER("기타");

    private final String koreanName;

    Category(String koreanName) {
        this.koreanName = koreanName;
    }

    public String koreanName() {
        return koreanName;
    }
}
