package com.musiccheck.domain.book.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_history")
@Getter
@NoArgsConstructor
public class UserHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false, length = 30)
    private String bookId;

    @Column(name = "search_query", length = 255)
    private String searchQuery;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UserHistory(Long userId, String bookId, String searchQuery) {
        this.userId = userId;
        this.bookId = bookId;
        this.searchQuery = searchQuery;
    }
}

