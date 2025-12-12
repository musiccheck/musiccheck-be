package com.musiccheck.domain.music.dto;

public record DislikedSongDto(
        Long feedbackId,      // 피드백 ID (취소 시 사용)
        String isbn,          // 책 ISBN (user_feedback.book_id에 저장된 값)
        String bookTitle,      // 책 제목
        String bookImage,      // 책 표지 이미지
        String trackId,        // 음악 트랙 ID
        String trackName,      // 곡 제목
        String artistName,     // 아티스트명
        String imageUrl        // 앨범 커버 이미지
) {}

