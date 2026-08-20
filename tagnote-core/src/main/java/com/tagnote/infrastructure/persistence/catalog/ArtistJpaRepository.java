package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.artist.ArtistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ArtistJpaRepository extends JpaRepository<ArtistEntity, Long> {

    List<ArtistEntity> findAllBySpotifyIdIn(Collection<String> spotifyIds);
}
