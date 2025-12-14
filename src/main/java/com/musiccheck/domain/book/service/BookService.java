package com.musiccheck.domain.book.service;

import com.musiccheck.domain.book.dto.BookDto;
import com.musiccheck.domain.book.entity.BookEntity;
import com.musiccheck.domain.book.entity.UserHistory;
import com.musiccheck.domain.book.repository.BookRepository;
import com.musiccheck.domain.book.repository.UserHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 사용자의 최근 읽은 책 목록 조회
     * @param userId 사용자 ID
     * @param limit 조회할 개수 (예: 6 또는 9)
     * @return 최근 읽은 책 목록 (최신순)
     */
    public List<BookDto> getRecentBooks(Long userId, int limit) {
        // 최근 읽은 책 ID 목록 조회 (중복 제거, 최신순)
        var pageable = PageRequest.of(0, limit);
        List<String> recentBookIds = userHistoryRepository.findRecentBookIdsByUserId(userId, pageable);

        if (recentBookIds.isEmpty()) {
            return List.of();
        }

        // book_id로 BookEntity 조회
        List<BookEntity> books = bookRepository.findByIsbnIn(recentBookIds);

        // BookDto로 변환 (순서 유지)
        return recentBookIds.stream()
                .map(bookId -> books.stream()
                        .filter(book -> book.getIsbn().equals(bookId))
                        .findFirst()
                        .map(BookDto::from)
                        .orElse(null))
                .filter(book -> book != null)
                .collect(Collectors.toList());
    }
}
