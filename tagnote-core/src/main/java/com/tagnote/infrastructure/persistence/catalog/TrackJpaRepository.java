package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.track.TrackEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrackJpaRepository extends JpaRepository<TrackEntity, Long> {

    @EntityGraph(attributePaths = "album")
    Optional<TrackEntity> findBySpotifyId(String spotifyId);

    @Query("""
            select track
            from TrackEntity track
            join fetch track.album
            where track.trackId = :trackId
            """)
    Optional<TrackEntity> findByIdWithAlbum(@Param("trackId") long trackId);
}
