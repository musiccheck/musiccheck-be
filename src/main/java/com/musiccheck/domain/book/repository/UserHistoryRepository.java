package com.musiccheck.domain.book.repository;

import com.musiccheck.domain.book.entity.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
    void deleteByUserId(Long userId);
}

