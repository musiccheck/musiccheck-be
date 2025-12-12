package com.musiccheck.domain.music.service;

import com.musiccheck.domain.music.dto.MusicDto;
import com.musiccheck.domain.music.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicRepository musicRepository;

    public List<MusicDto> recommend(String isbn) {

        // 1) NativeQuery로 추천된 음악 10개 받기
        List<Object[]> rows = musicRepository.recommendByIsbn(isbn);

        // 2) Object[] → DTO로 변환
        List<MusicDto> musicList = rows.stream()
                .map(r -> new MusicDto(
                        (String) r[0],  // track_id
                        (String) r[1],  // track_name
                        (String) r[2],  // artist_name
                        (String) r[3],  // image_url
                        (String) r[4]   // external_url
                ))
                .toList();

        // 3) playlist_generation_log 저장 (다음 단계에서 구현)
        // playlistLogService.save(userId, isbn, musicList);

        return musicList;
    }
}