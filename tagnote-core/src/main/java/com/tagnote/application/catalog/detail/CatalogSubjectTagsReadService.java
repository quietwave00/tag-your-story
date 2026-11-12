package com.tagnote.application.catalog.detail;

import com.tagnote.application.catalog.detail.model.SubjectTags;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogSubjectTagsReadService {
    private final TrackJpaRepository tracks;
    private final AlbumJpaRepository albums;
    private final TrackDetailReadService trackDetails;
    private final AlbumDetailReadService albumDetails;
    private final TagPreviewService previews;

    public SubjectTags getTrackTags(String spotifyTrackId) {
        TrackEntity track = tracks.findBySpotifyId(spotifyTrackId)
                .orElseThrow(() -> new CustomException(ExceptionCode.TRACK_NOT_FOUND));
        long trackId = track.getTrackId();
        return trackDetails.findResolvedTags(trackId)
                .map(SubjectTags::confirmed)
                .orElseGet(() -> SubjectTags.preview(
                        previews.fromPersisted(trackId, track.getAlbum().getAlbumId())));
    }

    public SubjectTags getAlbumTags(String spotifyAlbumId) {
        AlbumEntity album = albums.findBySpotifyId(spotifyAlbumId)
                .orElseThrow(() -> new CustomException(ExceptionCode.ALBUM_NOT_FOUND));
        long albumId = album.getAlbumId();
        return albumDetails.findResolvedTags(albumId)
                .map(SubjectTags::confirmed)
                .orElseGet(() -> SubjectTags.preview(previews.fromPersistedAlbum(albumId)));
    }
}
