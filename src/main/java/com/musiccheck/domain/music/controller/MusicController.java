package com.musiccheck.domain.music.controller;

import com.musiccheck.domain.music.dto.MusicDto;
import com.musiccheck.domain.music.service.MusicService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/{isbn}/playlist")
    public List<MusicDto> getPlaylist(@PathVariable String isbn) {
        return musicService.recommend(isbn);
    }
}