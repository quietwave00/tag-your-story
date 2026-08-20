package com.tagnote.domain.catalog.track;

import com.tagnote.core.domain.BaseTime;
import com.tagnote.domain.catalog.album.AlbumEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "track",
        uniqueConstraints = @UniqueConstraint(name = "uk_track_spotify_id", columnNames = "spotify_id"),
        indexes = {
                @Index(name = "idx_track_musicbrainz_id", columnList = "musicbrainz_id"),
                @Index(name = "idx_track_isrc", columnList = "isrc"),
                @Index(name = "idx_track_album_id", columnList = "album_id"),
                @Index(name = "idx_track_title", columnList = "title")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trackId;

    @Column(nullable = false)
    private String title;

    @Column(name = "spotify_id", nullable = false)
    private String spotifyId;

    @Column(name = "musicbrainz_id")
    private String musicbrainzId;

    private String isrc;

    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private AlbumEntity album;

    private TrackEntity(String title, String spotifyId, String isrc, Integer durationMs, AlbumEntity album) {
        this.title = title;
        this.spotifyId = spotifyId;
        this.isrc = isrc;
        this.durationMs = durationMs;
        this.album = album;
    }

    public static TrackEntity create(
            String title,
            String spotifyId,
            String isrc,
            Integer durationMs,
            AlbumEntity album
    ) {
        return new TrackEntity(title, spotifyId, isrc, durationMs, album);
    }
}
