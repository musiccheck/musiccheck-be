package com.musiccheck.domain.music.controller;

import com.musiccheck.domain.music.dto.MusicDto;
import com.musiccheck.domain.music.service.MusicService;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class MusicController {

    private final MusicService musicService;
    private final UserRepository userRepository;

    @GetMapping("/{isbn}/playlist")
    public List<MusicDto> getPlaylist(
            @PathVariable String isbn,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return musicService.recommend(isbn, user.getId());
    }
}