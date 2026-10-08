package com.tagnote.domain.catalog.artist;

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
        name = "artist",
        uniqueConstraints = @UniqueConstraint(name = "uk_artist_spotify_id", columnNames = "spotify_id"),
        indexes = {
                @Index(name = "idx_artist_musicbrainz_id", columnList = "musicbrainz_id"),
                @Index(name = "idx_artist_name", columnList = "name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long artistId;

    @Column(nullable = false)
    private String name;

    @Column(name = "spotify_id", nullable = false)
    private String spotifyId;

    @Column(name = "musicbrainz_id")
    private String musicbrainzId;

    private ArtistEntity(String name, String spotifyId) {
        this.name = name;
        this.spotifyId = spotifyId;
    }

    public static ArtistEntity create(String name, String spotifyId) {
        return new ArtistEntity(name, spotifyId);
    }
}
