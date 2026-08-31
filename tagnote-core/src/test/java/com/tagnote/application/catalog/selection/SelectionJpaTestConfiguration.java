package com.tagnote.application.catalog.selection;

import com.tagnote.domain.catalog.album.AlbumArtistEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagObservationEntity;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {
        ArtistEntity.class,
        AlbumEntity.class,
        AlbumArtistEntity.class,
        TrackEntity.class,
        TrackArtistEntity.class,
        TagEntity.class,
        TagAliasEntity.class,
        ExternalTagObservationEntity.class,
        TagAssertionEntity.class,
        SubjectTagResolvedEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        TrackJpaRepository.class,
        TagJpaRepository.class,
        TagAssertionJpaRepository.class,
        SubjectTagResolvedJpaRepository.class
})
class SelectionJpaTestConfiguration {
}
