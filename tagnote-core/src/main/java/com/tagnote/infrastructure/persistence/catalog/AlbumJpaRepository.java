package com.tagnote.infrastructure.persistence.catalog;

import com.tagnote.domain.catalog.album.AlbumEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AlbumJpaRepository extends JpaRepository<AlbumEntity, Long> {

    Optional<AlbumEntity> findBySpotifyId(String spotifyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select album from AlbumEntity album where album.albumId = :albumId")
    Optional<AlbumEntity> findByIdForUpdate(@Param("albumId") long albumId);
}
