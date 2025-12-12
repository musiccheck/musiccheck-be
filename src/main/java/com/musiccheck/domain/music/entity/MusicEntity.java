package com.musiccheck.domain.music.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "music")
@Getter
public class MusicEntity {

    @Id
    @Column(name = "track_id")
    private String trackId;

    @Column(name = "track_name")
    private String trackName;

    @Column(name = "artist_name")
    private String artistName;

    @Column(name = "album_name")
    private String albumName;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "external_url")
    private String externalUrl;

    private Integer popularity;

    @Column(name = "search_keyword")
    private String searchKeyword;

    @Column(name = "embedding_id")
    private Integer embeddingId;

    protected MusicEntity() {}  // JPA 기본 생성자

    public MusicEntity(
            String trackId,
            String trackName,
            String artistName,
            String albumName,
            String imageUrl,
            String externalUrl,
            Integer popularity,
            String searchKeyword,
            Integer embeddingId
    ) {
        this.trackId = trackId;
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.imageUrl = imageUrl;
        this.externalUrl = externalUrl;
        this.popularity = popularity;
        this.searchKeyword = searchKeyword;
        this.embeddingId = embeddingId;
    }
}