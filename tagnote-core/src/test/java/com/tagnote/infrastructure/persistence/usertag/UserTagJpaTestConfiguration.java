package com.tagnote.infrastructure.persistence.usertag;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.repository.UserTagRepository;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {
        UserEntity.class, BoardEntity.class, UserTagEntity.class, BoardUserTagEntity.class,
        AlbumEntity.class, ArtistEntity.class, TrackEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        BoardJpaRepository.class, UserTagRepository.class, BoardUserTagRepository.class
})
class UserTagJpaTestConfiguration {
}
