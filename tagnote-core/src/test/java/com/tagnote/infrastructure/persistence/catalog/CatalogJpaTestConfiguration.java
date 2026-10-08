package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {ArtistEntity.class, AlbumEntity.class, TrackEntity.class})
@EnableJpaRepositories(basePackageClasses = TrackJpaRepository.class)
class CatalogJpaTestConfiguration {
}
