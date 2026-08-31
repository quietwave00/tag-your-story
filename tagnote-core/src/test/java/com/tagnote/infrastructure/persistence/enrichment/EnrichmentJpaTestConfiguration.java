package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagObservationEntity;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagAliasJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {
        AlbumEntity.class,
        ArtistEntity.class,
        TrackEntity.class,
        TagEntity.class,
        TagAliasEntity.class,
        ExternalTagObservationEntity.class,
        TagAssertionEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        AlbumJpaRepository.class,
        TrackJpaRepository.class,
        TagJpaRepository.class,
        TagAliasJpaRepository.class,
        ExternalTagObservationJpaRepository.class,
        TagAssertionJpaRepository.class
})
class EnrichmentJpaTestConfiguration {
}
