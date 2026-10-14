package com.tagnote.infrastructure.persistence.board;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {
        BoardEntity.class,
        BoardUserTagEntity.class,
        UserEntity.class,
        UserTagEntity.class,
        AlbumEntity.class,
        ArtistEntity.class,
        TrackEntity.class
})
@EnableJpaRepositories(basePackageClasses = {BoardJpaRepository.class, BoardUserTagRepository.class})
class BoardJpaTestConfiguration {
}
