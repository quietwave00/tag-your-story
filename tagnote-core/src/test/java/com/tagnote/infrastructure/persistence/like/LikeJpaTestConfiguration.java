package com.tagnote.infrastructure.persistence.like;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.like.LikeEntity;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {LikeEntity.class, BoardEntity.class, UserEntity.class,
        AlbumEntity.class, ArtistEntity.class, TrackEntity.class})
@EnableJpaRepositories(basePackageClasses = {LikeJpaRepository.class})
class LikeJpaTestConfiguration {
}
