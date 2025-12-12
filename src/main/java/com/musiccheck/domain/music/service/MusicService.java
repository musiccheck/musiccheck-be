package com.musiccheck.domain.music.service;

import com.musiccheck.domain.music.dto.MusicDto;
import com.musiccheck.domain.music.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicRepository musicRepository;

    public List<MusicDto> recommend(String isbn, Long userId) {

        // 1) NativeQuery로 벡터 유사도 기준 추천된 음악 30개 받기 (좋아요 개수 포함)
        List<Object[]> rows = musicRepository.recommendByIsbn(isbn, userId);

        // 2) Object[] → DTO로 변환 (좋아요 개수 포함)
        List<MusicDto> musicList = rows.stream()
                .map(r -> new MusicDto(
                        (String) r[0],  // track_id
                        (String) r[1],  // track_name
                        (String) r[2],  // artist_name
                        (String) r[3],  // image_url
                        (String) r[4], // external_url
                        ((Number) r[5]).longValue()  // like_count
                ))
                .collect(Collectors.toList());

        // 3) 좋아요 개수 기준으로 내림차순 정렬
        musicList.sort(Comparator.comparing(MusicDto::likeCount).reversed());

        // 4) playlist_generation_log 저장 (다음 단계에서 구현)
        // playlistLogService.save(userId, isbn, musicList);

        return musicList;
    }
}