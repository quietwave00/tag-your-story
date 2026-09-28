package com.tagnote.domain.enrichment.observation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "track_tag_completion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackTagCompletionEntity {
    @Id
    @Column(name = "track_id")
    private Long trackId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public TrackTagCompletionEntity(long trackId) {
        if (trackId <= 0) throw new IllegalArgumentException("Track ID must be positive");
        this.trackId = trackId;
        this.completedAt = LocalDateTime.now();
    }
}
