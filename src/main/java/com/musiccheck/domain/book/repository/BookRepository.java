package com.musiccheck.domain.book.repository;

import com.musiccheck.domain.book.entity.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<BookEntity, String> {

    Page<BookEntity> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * ISBN 목록으로 책 조회
     */
    List<BookEntity> findByIsbnIn(List<String> isbns);
}