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
}
