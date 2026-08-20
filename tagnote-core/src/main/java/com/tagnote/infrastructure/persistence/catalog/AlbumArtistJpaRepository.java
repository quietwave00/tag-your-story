package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.album.AlbumArtistEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlbumArtistJpaRepository extends JpaRepository<AlbumArtistEntity, Long> {

    @EntityGraph(attributePaths = "artist")
    List<AlbumArtistEntity> findAllByAlbumAlbumIdOrderByPositionAsc(Long albumId);
}
