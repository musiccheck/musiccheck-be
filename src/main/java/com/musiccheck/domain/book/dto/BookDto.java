package com.musiccheck.domain.book.dto;

import com.musiccheck.domain.book.entity.BookEntity;

public record BookDto(
        String isbn,
        String title,
        String author,
        String publisher,
        String pubDate,
        String image
) {
    public static BookDto from(BookEntity e) {
        return new BookDto(
                e.getIsbn(),
                e.getTitle(),
                e.getAuthor(),
                e.getPublisher(),
                e.getPubdate(),
                e.getImage()
        );
    }
}