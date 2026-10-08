package com.tagnote.application.resolution;

import com.tagnote.application.resolution.config.TagResolutionProperties;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.AssertionSource;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagInheritanceServiceTest {

    @Mock private TagAssertionJpaRepository assertionRepository;
    @Mock private TagResolutionProperties properties;
    @InjectMocks private TagInheritanceService service;

    @Test
    void album_assertion_confidence에_설정_weight를_곱해_inherited_assertion을_생성한다() {
        TrackEntity track = persistedTrack();
        TagEntity tag = activeTag(10L, "ambient");
        TagAssertionEntity albumAssertion = persistedAssertion(100L, TagAssertionEntity.createApproved(
                SubjectRef.album(track.getAlbum()), tag,
                AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.80
        ));
        when(properties.getAlbumToTrackInheritanceWeight()).thenReturn(0.85);
        when(assertionRepository.findApprovedDirectBySubject(SubjectType.ALBUM, 1L))
                .thenReturn(List.of(albumAssertion));
        when(assertionRepository.findInheritedBySubject(SubjectType.TRACK, 2L))
                .thenReturn(List.of());

        service.synchronizeAlbumInheritance(track, List.of());

        ArgumentCaptor<TagAssertionEntity> captor = ArgumentCaptor.forClass(TagAssertionEntity.class);
        verify(assertionRepository).save(captor.capture());
        assertThat(captor.getValue()).satisfies(inherited -> {
            assertThat(inherited.subjectRef()).isEqualTo(SubjectRef.track(2L));
            assertThat(inherited.getTag().getTagId()).isEqualTo(tag.getTagId());
            assertThat(inherited.getConfidence()).isCloseTo(0.68, offset(0.000001));
            assertThat(inherited.getInheritedFromAssertion().getAssertionId()).isEqualTo(100L);
        });
    }

    @Test
    void track_direct와_같은_identity는_unique_충돌을_피하기_위해_inherited를_생성하지_않는다() {
        TrackEntity track = persistedTrack();
        TagEntity tag = activeTag(10L, "ambient");
        TagAssertionEntity albumAssertion = persistedAssertion(100L, TagAssertionEntity.createApproved(
                SubjectRef.album(track.getAlbum()), tag,
                AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.80
        ));
        TagAssertionEntity trackDirect = persistedAssertion(200L, TagAssertionEntity.createApproved(
                SubjectRef.track(track), tag,
                AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.90
        ));
        when(properties.getAlbumToTrackInheritanceWeight()).thenReturn(0.85);
        when(assertionRepository.findApprovedDirectBySubject(SubjectType.ALBUM, 1L))
                .thenReturn(List.of(albumAssertion));
        when(assertionRepository.findInheritedBySubject(SubjectType.TRACK, 2L))
                .thenReturn(List.of());

        service.synchronizeAlbumInheritance(track, List.of(trackDirect));

        verify(assertionRepository, never()).save(any());
    }

    @Test
    void 유효하지_않은_inherited_assertion은_cleanup한다() {
        TrackEntity track = persistedTrack();
        TagEntity tag = activeTag(10L, "ambient");
        TagAssertionEntity parent = persistedAssertion(100L, TagAssertionEntity.createApproved(
                SubjectRef.album(track.getAlbum()), tag,
                AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.80
        ));
        TagAssertionEntity stale = persistedAssertion(200L, TagAssertionEntity.createInheritedApproved(
                SubjectRef.track(track), parent, 0.68
        ));
        when(properties.getAlbumToTrackInheritanceWeight()).thenReturn(0.85);
        when(assertionRepository.findApprovedDirectBySubject(SubjectType.ALBUM, 1L))
                .thenReturn(List.of());
        when(assertionRepository.findInheritedBySubject(SubjectType.TRACK, 2L))
                .thenReturn(List.of(stale));

        service.synchronizeAlbumInheritance(track, List.of());

        verify(assertionRepository).deleteAllInBatch(List.of(stale));
    }

    @Test
    void inherited_assertion을_다시_parent로_상속하지_않는다() {
        TrackEntity track = persistedTrack();
        TagEntity tag = activeTag(10L, "ambient");
        TagAssertionEntity parent = persistedAssertion(100L, TagAssertionEntity.createApproved(
                SubjectRef.album(track.getAlbum()), tag,
                AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.80
        ));
        TagAssertionEntity inherited = persistedAssertion(200L, TagAssertionEntity.createInheritedApproved(
                SubjectRef.track(track), parent, 0.68
        ));

        assertThatThrownBy(() -> TagAssertionEntity.createInheritedApproved(
                SubjectRef.track(3L), inherited, 0.50
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private TrackEntity persistedTrack() {
        AlbumEntity album = AlbumEntity.create("Album", "album", 2026);
        ReflectionTestUtils.setField(album, "albumId", 1L);
        TrackEntity track = TrackEntity.create("Track", "track", "ISRC", 180_000, album);
        ReflectionTestUtils.setField(track, "trackId", 2L);
        return track;
    }

    private TagEntity activeTag(long id, String slug) {
        TagEntity tag = TagEntity.create(slug, slug, TagType.GENRE, TagStatus.ACTIVE, null);
        ReflectionTestUtils.setField(tag, "tagId", id);
        return tag;
    }

    private TagAssertionEntity persistedAssertion(long id, TagAssertionEntity assertion) {
        ReflectionTestUtils.setField(assertion, "assertionId", id);
        return assertion;
    }
}
