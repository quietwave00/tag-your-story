package com.tagnote.domain.catalog.album;

import com.tagnote.core.domain.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "album",
        uniqueConstraints = @UniqueConstraint(name = "uk_album_spotify_id", columnNames = "spotify_id"),
        indexes = {
                @Index(name = "idx_album_musicbrainz_id", columnList = "musicbrainz_id"),
                @Index(name = "idx_album_title", columnList = "title")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long albumId;

    @Column(nullable = false)
    private String title;

    @Column(name = "spotify_id", nullable = false)
    private String spotifyId;

    @Column(name = "musicbrainz_id")
    private String musicbrainzId;

    @Column(name = "release_year")
    private Integer releaseYear;

    private AlbumEntity(String title, String spotifyId, Integer releaseYear) {
        this.title = title;
        this.spotifyId = spotifyId;
        this.releaseYear = releaseYear;
    }

    public static AlbumEntity create(String title, String spotifyId, Integer releaseYear) {
        return new AlbumEntity(title, spotifyId, releaseYear);
    }
}
