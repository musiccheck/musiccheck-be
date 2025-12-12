package com.musiccheck.domain.book.repository;

import com.musiccheck.domain.book.entity.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<BookEntity, String> {

    Page<BookEntity> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
}