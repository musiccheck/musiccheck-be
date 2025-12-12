package com.musiccheck.domain.book.controller;

import com.musiccheck.domain.book.dto.BookDto;
import com.musiccheck.domain.book.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class BookController {

    private final BookService bookService;

    @GetMapping
    public Page<BookDto> search(
            @RequestParam String keyword,                 // 검색어
            @RequestParam(defaultValue = "0") int page   // 페이지 번호
    ) {
        int size = 12; // 3x4 그리드 → 12개
        return bookService.search(keyword, page, size);
    }
}
