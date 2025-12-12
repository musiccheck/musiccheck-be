package com.musiccheck.domain.music.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_feedback", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "book_id", "music_id"}))
@Getter
@NoArgsConstructor
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false, length = 30)
    private String bookId;

    @Column(name = "music_id", nullable = false, length = 50)
    private String musicId;

    @Column(name = "feedback", nullable = false, length = 10)
    private String feedback; // 'like' 또는 'dislike'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UserFeedback(Long userId, String bookId, String musicId, String feedback) {
        this.userId = userId;
        this.bookId = bookId;
        this.musicId = musicId;
        this.feedback = feedback;
    }

    public void updateFeedback(String feedback) {
        this.feedback = feedback;
    }
}

