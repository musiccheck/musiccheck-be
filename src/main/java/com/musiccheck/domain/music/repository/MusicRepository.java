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
            m.external_url
        FROM music m
        JOIN embedding em ON em.embedding_id = m.embedding_id
        ORDER BY em.vector <#> (
            SELECT vector
            FROM embedding
            WHERE embedding_id = (
                SELECT embedding_id
                FROM book
                WHERE isbn = :isbn
            )
        )
        LIMIT 10
        """,
            nativeQuery = true)
    List<Object[]> recommendByIsbn(@Param("isbn") String isbn);
}