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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicRepository musicRepository;
    private final UserFeedbackRepository userFeedbackRepository;
    private final BookRepository bookRepository;

    public List<MusicDto> recommend(String isbn, Long userId) {
        // 가중치 설정: 유사도 0.7, 좋아요 0.3
        final double SIMILARITY_WEIGHT = 0.7;
        final double LIKE_WEIGHT = 0.3;

        // 1) 사용자의 싫어요 곡 목록 먼저 조회 (필터링용)
        final Set<String> dislikedTrackIds;
        if (userId != null) {
            List<UserFeedback> dislikedFeedbacks = userFeedbackRepository.findByUserIdAndBookIdAndFeedback(userId, isbn, "dislike");
            dislikedTrackIds = dislikedFeedbacks.stream()
                    .map(UserFeedback::getMusicId)
                    .collect(Collectors.toSet());
        } else {
            dislikedTrackIds = new HashSet<>();
        }

        // 2) NativeQuery로 전체 음악 조회 (좋아요 개수, 유사도 점수 포함)
        List<Object[]> rows = musicRepository.recommendByIsbn(isbn, userId);

        // 3) Object[] → 임시 데이터 구조로 변환 및 싫어요 필터링
        List<MusicWithScore> musicWithScores = rows.stream()
                .map(r -> new MusicWithScore(
                        (String) r[0],  // track_id
                        (String) r[1],  // track_name
                        (String) r[2],  // artist_name
                        (String) r[3],  // image_url
                        (String) r[4], // external_url
                        ((Number) r[5]).longValue(),  // like_count
                        ((Number) r[6]).doubleValue()  // similarity_score (negative inner product, 작을수록 유사)
                ))
                .filter(m -> !dislikedTrackIds.contains(m.trackId))  // 싫어요 곡 제외
                .collect(Collectors.toList());

        // 4) 유사도 점수와 좋아요 개수의 최소/최대값 찾기 (정규화용)
        double minSimilarity = musicWithScores.stream()
                .mapToDouble(m -> m.similarityScore)
                .min()
                .orElse(0.0);
        double maxSimilarity = musicWithScores.stream()
                .mapToDouble(m -> m.similarityScore)
                .max()
                .orElse(1.0);
        long maxLikeCount = musicWithScores.stream()
                .mapToLong(m -> m.likeCount)
                .max()
                .orElse(1L);

        // 5) 가중치 기반 점수 계산 및 정렬 후 30개만 반환
        List<MusicDto> musicList = musicWithScores.stream()
                .map(m -> {
                    // 유사도 점수 정규화 (0-1 범위, 작을수록 유사하므로 역변환)
                    double normalizedSimilarity = maxSimilarity > minSimilarity
                            ? 1.0 - ((m.similarityScore - minSimilarity) / (maxSimilarity - minSimilarity))
                            : 1.0;
                    
                    // 좋아요 개수 정규화 (0-1 범위)
                    double normalizedLike = maxLikeCount > 0
                            ? (double) m.likeCount / maxLikeCount
                            : 0.0;
                    
                    // 가중치 기반 최종 점수 계산
                    double finalScore = (normalizedSimilarity * SIMILARITY_WEIGHT) + (normalizedLike * LIKE_WEIGHT);
                    
                    return new MusicWithFinalScore(
                            new MusicDto(
                                    m.trackId,
                                    m.trackName,
                                    m.artistName,
                                    m.imageUrl,
                                    m.externalUrl,
                                    m.likeCount
                            ),
                            finalScore
                    );
                })
                .sorted(Comparator.comparing((MusicWithFinalScore m) -> m.finalScore).reversed())
                .limit(30)  // 최종 30개만 반환
                .map(m -> m.dto)
                .collect(Collectors.toList());

        // 6) playlist_generation_log 저장 (다음 단계에서 구현)
        // playlistLogService.save(userId, isbn, musicList);

        return musicList;
    }

    // 임시 데이터 구조 (유사도 점수 포함)
    private static class MusicWithScore {
        String trackId;
        String trackName;
        String artistName;
        String imageUrl;
        String externalUrl;
        long likeCount;
        double similarityScore;

        MusicWithScore(String trackId, String trackName, String artistName, String imageUrl, 
                      String externalUrl, long likeCount, double similarityScore) {
            this.trackId = trackId;
            this.trackName = trackName;
            this.artistName = artistName;
            this.imageUrl = imageUrl;
            this.externalUrl = externalUrl;
            this.likeCount = likeCount;
            this.similarityScore = similarityScore;
        }
    }

    // 최종 점수 포함 데이터 구조
    private static class MusicWithFinalScore {
        MusicDto dto;
        double finalScore;

        MusicWithFinalScore(MusicDto dto, double finalScore) {
            this.dto = dto;
            this.finalScore = finalScore;
        }
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

    // 좋아요 목록 조회 (특정 책)
    public List<Map<String, Object>> getLikedSongs(Long userId, String bookId) {
        // 1) 유저의 특정 책에 대한 좋아요 피드백 목록 조회
        List<UserFeedback> likedFeedbacks = userFeedbackRepository.findByUserIdAndBookIdAndFeedback(userId, bookId, "like");

        // 2) 각 피드백을 Map으로 변환 (프론트엔드 요구사항에 맞춤)
        return likedFeedbacks.stream()
                .map(feedback -> {
                    Map<String, Object> likedSong = new HashMap<>();
                    likedSong.put("musicId", feedback.getMusicId());
                    likedSong.put("trackId", feedback.getMusicId()); // trackId와 musicId 동일
                    likedSong.put("bookId", feedback.getBookId());
                    likedSong.put("isbn", feedback.getBookId()); // bookId와 isbn 동일
                    likedSong.put("feedbackId", feedback.getFeedbackId());
                    return likedSong;
                })
                .collect(Collectors.toList());
    }

    // 싫어요 목록 조회
    public List<DislikedSongDto> getDislikedSongs(Long userId) {
        // 1) 유저의 싫어요 피드백 목록 조회
        List<UserFeedback> dislikedFeedbacks = userFeedbackRepository.findByUserIdAndFeedback(userId, "dislike");

        // 2) 각 피드백에 대해 책과 음악 정보 조회하여 DTO로 변환
        return dislikedFeedbacks.stream()
                .map(feedback -> {
                    String isbn = feedback.getBookId(); // user_feedback.book_id는 실제로 isbn 값을 저장
                    BookEntity book = bookRepository.findById(isbn)
                            .orElse(null);
                    MusicEntity music = musicRepository.findById(feedback.getMusicId())
                            .orElse(null);

                    // 음악 정보가 없으면 제외 (책 정보는 없어도 isbn은 포함)
                    if (music == null) {
                        return null;
                    }

                    // 책 정보가 없어도 isbn은 포함하여 반환
                    return new DislikedSongDto(
                            feedback.getFeedbackId(),
                            isbn, // isbn은 항상 포함 (user_feedback.book_id 값)
                            book != null ? book.getTitle() : null,
                            book != null ? book.getImage() : null,
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