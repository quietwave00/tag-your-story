package com.tagnote.domain.catalog.album;

import com.tagnote.domain.catalog.artist.ArtistEntity;
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
        name = "album_artist",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_album_artist", columnNames = {"album_id", "artist_id"}),
                @UniqueConstraint(name = "uk_album_artist_position", columnNames = {"album_id", "position"})
        },
        indexes = @Index(name = "idx_album_artist_artist_album", columnList = "artist_id, album_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long albumArtistId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private AlbumEntity album;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private ArtistEntity artist;

    private int position;

    private AlbumArtistEntity(AlbumEntity album, ArtistEntity artist, int position) {
        this.album = album;
        this.artist = artist;
        this.position = position;
    }

    public static AlbumArtistEntity create(AlbumEntity album, ArtistEntity artist, int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Artist position must not be negative");
        }
        return new AlbumArtistEntity(album, artist, position);
    }
}
