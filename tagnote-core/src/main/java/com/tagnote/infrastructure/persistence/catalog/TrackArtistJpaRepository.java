package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.track.TrackArtistEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackArtistJpaRepository extends JpaRepository<TrackArtistEntity, Long> {

    @EntityGraph(attributePaths = "artist")
    List<TrackArtistEntity> findAllByTrackTrackIdOrderByPositionAsc(Long trackId);
}
