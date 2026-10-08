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
@Table(name = "album_tag_completion")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumTagCompletionEntity {
    @Id
    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public AlbumTagCompletionEntity(long albumId) {
        if (albumId <= 0) throw new IllegalArgumentException("Album ID must be positive");
        this.albumId = albumId;
        this.completedAt = LocalDateTime.now();
    }
}
