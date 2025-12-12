package com.musiccheck.domain.music.controller;

import com.musiccheck.domain.music.dto.DislikedSongDto;
import com.musiccheck.domain.music.dto.MusicDto;
import com.musiccheck.domain.music.service.MusicService;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;
    private final UserRepository userRepository;

    @GetMapping("/api/books/{isbn}/playlist")
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

    // 좋아요/싫어요 등록
    @PostMapping("/api/likes")
    public ResponseEntity<Map<String, Object>> saveFeedback(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        String bookId = request.get("bookId");
        String musicId = request.get("musicId");
        String feedback = request.get("feedback"); // 'like' 또는 'dislike'

        if (bookId == null || musicId == null || feedback == null) {
            throw new IllegalArgumentException("bookId, musicId, feedback은 필수입니다.");
        }

        if (!feedback.equals("like") && !feedback.equals("dislike")) {
            throw new IllegalArgumentException("feedback은 'like' 또는 'dislike'여야 합니다.");
        }

        musicService.saveFeedback(user.getId(), bookId, musicId, feedback);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "피드백이 저장되었습니다.");
        
        // 싫어요인 경우 제외된 곡 정보 반환 (프론트에서 즉시 제거하기 위해)
        if (feedback.equals("dislike")) {
            response.put("removedTrackId", musicId);
        }

        return ResponseEntity.ok(response);
    }

    // 싫어요 목록 조회
    @GetMapping("/api/likes/disliked")
    public ResponseEntity<List<DislikedSongDto>> getDislikedSongs(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        List<DislikedSongDto> dislikedSongs = musicService.getDislikedSongs(user.getId());
        return ResponseEntity.ok(dislikedSongs);
    }

    // 싫어요 취소
    @DeleteMapping("/api/likes/{feedbackId}")
    public ResponseEntity<Map<String, Object>> deleteFeedback(
            @PathVariable Long feedbackId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        musicService.deleteFeedback(user.getId(), feedbackId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "싫어요가 취소되었습니다.");

        return ResponseEntity.ok(response);
    }
}