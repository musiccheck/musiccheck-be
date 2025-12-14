package com.musiccheck.domain.book.repository;

import com.musiccheck.domain.book.entity.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
    void deleteByUserId(Long userId);

    /**
     * 사용자의 최근 읽은 책 ID 목록 조회 (중복 제거, 최신순)
     * @param userId 사용자 ID
     * @param limit 조회할 개수 (예: 6 또는 9)
     * @return 최근 읽은 책 ID 목록 (최신순)
     */
    @Query("SELECT h.bookId FROM UserHistory h " +
           "WHERE h.userId = :userId " +
           "AND h.id IN (SELECT MAX(h2.id) FROM UserHistory h2 WHERE h2.userId = :userId GROUP BY h2.bookId) " +
           "ORDER BY h.createdAt DESC")
    List<String> findRecentBookIdsByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
}

