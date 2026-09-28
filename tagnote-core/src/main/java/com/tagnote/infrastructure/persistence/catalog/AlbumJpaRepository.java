package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.album.AlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlbumJpaRepository extends JpaRepository<AlbumEntity, Long> {

    Optional<AlbumEntity> findBySpotifyId(String spotifyId);
}
