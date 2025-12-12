package com.musiccheck.domain.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "book")
@Getter
public class BookEntity {

    @Id
    private String isbn;

    private String title;
    private String author;
    private String publisher;
    private String pubdate;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String image;

    private String searchKeyword;

    private Integer embeddingId;

    protected BookEntity() {} // JPA 기본 생성자 (필수)

    public BookEntity(
            String isbn,
            String title,
            String author,
            String publisher,
            String pubdate,
            String description,
            String image,
            String searchKeyword,
            Integer embeddingId
    ) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.pubdate = pubdate;
        this.description = description;
        this.image = image;
        this.searchKeyword = searchKeyword;
        this.embeddingId = embeddingId;
    }
}