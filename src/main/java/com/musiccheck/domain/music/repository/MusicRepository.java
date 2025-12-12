package com.musiccheck.domain.music.repository;

import com.musiccheck.domain.music.entity.MusicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MusicRepository extends JpaRepository<MusicEntity, String> {

    @Query(value = """
        SELECT 
            m.track_id,
            m.track_name,
            m.artist_name,
            m.image_url,
            m.external_url,
            COALESCE(COUNT(CASE WHEN uf.feedback = 'like' THEN 1 END), 0) as like_count
        FROM music m
        JOIN embedding em ON em.embedding_id = m.embedding_id
        LEFT JOIN user_feedback uf ON uf.music_id = m.track_id AND uf.book_id = :isbn
        WHERE (:userId IS NULL OR NOT EXISTS (
            SELECT 1 
            FROM user_feedback uf_dislike 
            WHERE uf_dislike.user_id = :userId 
            AND uf_dislike.book_id = :isbn 
            AND uf_dislike.music_id = m.track_id 
            AND uf_dislike.feedback = 'dislike'
        ))
        GROUP BY m.track_id, m.track_name, m.artist_name, m.image_url, m.external_url, em.vector
        ORDER BY em.vector <#> (
            SELECT vector
            FROM embedding
            WHERE embedding_id = (
                SELECT embedding_id
                FROM book
                WHERE isbn = :isbn
            )
        )
        LIMIT 30
        """,
            nativeQuery = true)
    List<Object[]> recommendByIsbn(@Param("isbn") String isbn, @Param("userId") Long userId);
}