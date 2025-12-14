package com.musiccheck.domain.book.controller;

import com.musiccheck.domain.book.dto.BookDto;
import com.musiccheck.domain.book.service.BookService;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class BookController {

    private final BookService bookService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<BookDto> search(
            @RequestParam String keyword,                 // 검색어
            @RequestParam(defaultValue = "0") int page   // 페이지 번호
    ) {
        int size = 12; // 3x4 그리드 → 12개
        return bookService.search(keyword, page, size);
    }

    // 책 선택 시 히스토리 저장
    @PostMapping("/books/{isbn}/select")
    public ResponseEntity<Map<String, Object>> selectBook(
            @PathVariable String isbn,
            @RequestParam String searchQuery,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        bookService.saveHistory(user.getId(), isbn, searchQuery);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "히스토리가 저장되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 최근 읽은 책 목록 조회
     * @param limit 조회할 개수 (기본값: 6, 3x2 그리드)
     * @return 최근 읽은 책 목록
     */
    @GetMapping("/recent")
    public ResponseEntity<List<BookDto>> getRecentBooks(
            @RequestParam(defaultValue = "6") int limit,  // 기본값: 6개 (3x2)
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // limit은 6 또는 9만 허용 (3x2 또는 3x3)
        if (limit != 6 && limit != 9) {
            limit = 6; // 기본값으로 설정
        }

        List<BookDto> recentBooks = bookService.getRecentBooks(user.getId(), limit);
        return ResponseEntity.ok(recentBooks);
    }
}
