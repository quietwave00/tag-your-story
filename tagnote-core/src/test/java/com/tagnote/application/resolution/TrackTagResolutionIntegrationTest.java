package com.tagnote.application.resolution;

import com.tagnote.application.resolution.config.TagResolutionProperties;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.AssertionSource;
import com.tagnote.domain.enrichment.assertion.AssertionStatus;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.CanonicalTagService;
import com.tagnote.domain.resolution.ResolutionReason;
import com.tagnote.domain.resolution.TagResolver;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import com.tagnote.infrastructure.persistence.resolution.HibernateResolutionConflictTranslator;
import com.tagnote.infrastructure.persistence.resolution.ResolutionJpaTestConfiguration;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@DataJpaTest
@ContextConfiguration(classes = ResolutionJpaTestConfiguration.class)
@Import({
        TagResolutionService.class,
        TagResolutionWriteService.class,
        TagInheritanceService.class,
        HibernateResolutionConflictTranslator.class,
        CanonicalTagService.class,
        TagResolver.class,
        TagResolutionProperties.class
})
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "tag.resolution.minimum-score=0.50",
        "tag.resolution.album-to-track-inheritance-weight=0.85"
})
class TrackTagResolutionIntegrationTest {

    @Autowired private TagResolutionService resolutionService;
    @Autowired private SubjectTagResolvedJpaRepository resolvedRepository;
    @Autowired private TagAssertionJpaRepository assertionRepository;
    @Autowired private TagJpaRepository tagRepository;
    @Autowired private TrackJpaRepository trackRepository;
    @Autowired private AlbumJpaRepository albumRepository;
    @Autowired private EntityManager entityManager;

    private AlbumEntity album;
    private TrackEntity track;

    @BeforeEach
    void setUp() {
        album = albumRepository.saveAndFlush(AlbumEntity.create("Album", "inheritance-album", 2026));
        track = trackRepository.saveAndFlush(TrackEntity.create(
                "Track", "inheritance-track", "ISRC-I", 180_000, album
        ));
    }

    @Test
    void album_approved_direct_assertion을_track_inherited_assertion과_projection으로_반영한다() {
        TagEntity tag = activeTag("ambient-inherited");
        TagAssertionEntity albumAssertion = assertionRepository.saveAndFlush(TagAssertionEntity.createApproved(
                SubjectRef.album(album), tag, AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.80
        ));

        var result = resolutionService.resolve(SubjectRef.track(track));
        entityManager.clear();

        assertThat(result).singleElement().satisfies(resolved -> {
            assertThat(resolved.tagId()).isEqualTo(tag.getTagId());
            assertThat(resolved.score()).isCloseTo(0.68, offset(0.000001));
            assertThat(resolved.reason()).isEqualTo(ResolutionReason.INHERITED_FROM_ALBUM);
        });
        assertThat(assertionRepository.findInheritedBySubject(SubjectType.TRACK, track.getTrackId()))
                .singleElement()
                .satisfies(inherited -> {
                    assertThat(inherited.getInheritedFromAssertion().getAssertionId())
                            .isEqualTo(albumAssertion.getAssertionId());
                    assertThat(inherited.getConfidence()).isCloseTo(0.68, offset(0.000001));
                });
    }

    @Test
    void 반복_resolve해도_inherited_assertion과_resolved_row가_증가하지_않는다() {
        TagEntity tag = activeTag("idempotent-inherited");
        assertionRepository.saveAndFlush(TagAssertionEntity.createApproved(
                SubjectRef.album(album), tag, AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.80
        ));

        resolutionService.resolve(SubjectRef.track(track));
        resolutionService.resolve(SubjectRef.track(track));

        assertThat(assertionRepository.findInheritedBySubject(SubjectType.TRACK, track.getTrackId())).hasSize(1);
        assertThat(resolvedRepository.findAllBySubjectWithTag(SubjectType.TRACK, track.getTrackId())).hasSize(1);
    }

    @Test
    void album_assertion이_삭제되면_stale_inherited와_auto_projection을_정리한다() {
        TagEntity tag = activeTag("stale-inherited");
        TagAssertionEntity albumAssertion = assertionRepository.saveAndFlush(TagAssertionEntity.createApproved(
                SubjectRef.album(album), tag, AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 0.80
        ));
        resolutionService.resolve(SubjectRef.track(track));

        assertionRepository.delete(albumAssertion);
        assertionRepository.flush();
        assertThat(resolutionService.resolve(SubjectRef.track(track))).isEmpty();

        assertThat(assertionRepository.findInheritedBySubject(SubjectType.TRACK, track.getTrackId())).isEmpty();
        assertThat(resolvedRepository.findAllBySubjectWithTag(SubjectType.TRACK, track.getTrackId())).isEmpty();
    }

    @Test
    void 같은_canonical_tag의_track_direct가_있으면_projection은_direct를_사용한다() {
        TagEntity tag = activeTag("direct-wins");
        assertionRepository.saveAndFlush(TagAssertionEntity.createApproved(
                SubjectRef.album(album), tag, AssertionSource.DISCOGS, EvidenceType.EXPLICIT_STYLE, 1.0
        ));
        assertionRepository.saveAndFlush(TagAssertionEntity.createApproved(
                SubjectRef.track(track), tag, AssertionSource.MUSICBRAINZ, EvidenceType.EXPLICIT_GENRE, 0.60
        ));

        assertThat(resolutionService.resolve(SubjectRef.track(track)))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.tagId()).isEqualTo(tag.getTagId());
                    assertThat(result.score()).isEqualTo(0.60);
                    assertThat(result.reason()).isEqualTo(ResolutionReason.AUTO);
                });
    }

    private TagEntity activeTag(String slug) {
        return tagRepository.saveAndFlush(TagEntity.create(
                slug, slug, TagType.GENRE, TagStatus.ACTIVE, null
        ));
    }
}
