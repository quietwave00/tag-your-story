package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.track.TrackEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackJpaRepository extends JpaRepository<TrackEntity, Long> {

    @EntityGraph(attributePaths = "album")
    Optional<TrackEntity> findBySpotifyId(String spotifyId);
}
