package com.musiccheck.domain.book.service;

import com.musiccheck.domain.book.dto.BookDto;
import com.musiccheck.domain.book.entity.UserHistory;
import com.musiccheck.domain.book.repository.BookRepository;
import com.musiccheck.domain.book.repository.UserHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final UserHistoryRepository userHistoryRepository;

    // 검색 메서드
    public Page<BookDto> search(String keyword, int page, int size) {

        // page: 0부터 시작
        var pageable = PageRequest.of(page, size);

        return bookRepository
                .findByTitleContainingIgnoreCase(keyword, pageable)
                .map(BookDto::from);  // Entity → DTO 변환
    }

    // 책 선택 시 히스토리 저장
    @Transactional
    public void saveHistory(Long userId, String bookId, String searchQuery) {
        UserHistory history = new UserHistory(userId, bookId, searchQuery);
        userHistoryRepository.save(history);
    }
}
