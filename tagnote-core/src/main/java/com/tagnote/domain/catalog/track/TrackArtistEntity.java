package com.tagnote.domain.catalog.track;

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
        name = "track_artist",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_track_artist", columnNames = {"track_id", "artist_id"}),
                @UniqueConstraint(name = "uk_track_artist_position", columnNames = {"track_id", "position"})
        },
        indexes = @Index(name = "idx_track_artist_artist_track", columnList = "artist_id, track_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long trackArtistId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private TrackEntity track;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private ArtistEntity artist;

    private int position;

    private TrackArtistEntity(TrackEntity track, ArtistEntity artist, int position) {
        this.track = track;
        this.artist = artist;
        this.position = position;
    }

    public static TrackArtistEntity create(TrackEntity track, ArtistEntity artist, int position) {
        if (position < 0) {
            throw new IllegalArgumentException("Artist position must not be negative");
        }
        return new TrackArtistEntity(track, artist, position);
    }
}
