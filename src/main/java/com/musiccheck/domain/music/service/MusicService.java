package com.musiccheck.domain.music.service;

import com.musiccheck.domain.book.entity.BookEntity;
import com.musiccheck.domain.book.repository.BookRepository;
import com.musiccheck.domain.music.dto.DislikedSongDto;
import com.musiccheck.domain.music.dto.MusicDto;
import com.musiccheck.domain.music.entity.MusicEntity;
import com.musiccheck.domain.music.entity.UserFeedback;
import com.musiccheck.domain.music.repository.MusicRepository;
import com.musiccheck.domain.music.repository.UserFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicRepository musicRepository;
    private final UserFeedbackRepository userFeedbackRepository;
    private final BookRepository bookRepository;

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

    // 좋아요/싫어요 저장 또는 업데이트
    @Transactional
    public void saveFeedback(Long userId, String bookId, String musicId, String feedback) {
        // 기존 피드백이 있는지 확인
        userFeedbackRepository.findByUserIdAndBookIdAndMusicId(userId, bookId, musicId)
                .ifPresentOrElse(
                        existingFeedback -> {
                            // 기존 피드백이 있으면 feedback만 업데이트
                            existingFeedback.updateFeedback(feedback);
                            userFeedbackRepository.save(existingFeedback);
                        },
                        () -> {
                            // 기존 피드백이 없으면 새로 생성
                            UserFeedback newFeedback = new UserFeedback(userId, bookId, musicId, feedback);
                            userFeedbackRepository.save(newFeedback);
                        }
                );
    }

    // 싫어요 목록 조회
    public List<DislikedSongDto> getDislikedSongs(Long userId) {
        // 1) 유저의 싫어요 피드백 목록 조회
        List<UserFeedback> dislikedFeedbacks = userFeedbackRepository.findByUserIdAndFeedback(userId, "dislike");

        // 2) 각 피드백에 대해 책과 음악 정보 조회하여 DTO로 변환
        return dislikedFeedbacks.stream()
                .map(feedback -> {
                    BookEntity book = bookRepository.findById(feedback.getBookId())
                            .orElse(null);
                    MusicEntity music = musicRepository.findById(feedback.getMusicId())
                            .orElse(null);

                    if (book == null || music == null) {
                        return null; // 책이나 음악이 없으면 제외
                    }

                    return new DislikedSongDto(
                            feedback.getFeedbackId(),
                            book.getIsbn(),
                            book.getTitle(),
                            book.getImage(),
                            music.getTrackId(),
                            music.getTrackName(),
                            music.getArtistName(),
                            music.getImageUrl()
                    );
                })
                .filter(dto -> dto != null) // null 제외
                .collect(Collectors.toList());
    }

    // 싫어요 취소 (피드백 삭제)
    @Transactional
    public void deleteFeedback(Long userId, Long feedbackId) {
        UserFeedback feedback = userFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));

        // 본인의 피드백인지 확인
        if (!feedback.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 피드백만 삭제할 수 있습니다.");
        }

        userFeedbackRepository.delete(feedback);
    }
}