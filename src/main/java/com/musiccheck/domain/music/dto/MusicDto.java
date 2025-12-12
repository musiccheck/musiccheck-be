package com.musiccheck.domain.music.dto;

public record MusicDto(
        String trackId,
        String trackName,
        String artistName,
        String imageUrl,
        String externalUrl,
        Long likeCount
) {}