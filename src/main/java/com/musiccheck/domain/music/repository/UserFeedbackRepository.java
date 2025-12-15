package com.musiccheck.domain.music.repository;

import com.musiccheck.domain.music.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
    Optional<UserFeedback> findByUserIdAndBookIdAndMusicId(Long userId, String bookId, String musicId);
    List<UserFeedback> findByUserIdAndFeedback(Long userId, String feedback);
    List<UserFeedback> findByUserIdAndBookIdAndFeedback(Long userId, String bookId, String feedback);
    void deleteByUserId(Long userId);
}

