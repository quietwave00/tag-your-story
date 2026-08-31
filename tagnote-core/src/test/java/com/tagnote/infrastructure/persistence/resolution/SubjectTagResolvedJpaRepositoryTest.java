package com.tagnote.infrastructure.persistence.resolution;

import com.tagnote.application.resolution.TagInheritanceService;
import com.tagnote.application.resolution.TagResolutionService;
import com.tagnote.application.resolution.TagResolutionWriteService;
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
import com.tagnote.domain.resolution.ResolvedStatus;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.resolution.TagResolver;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "tag.resolution.minimum-score=0.50",
        "tag.resolution.album-to-track-inheritance-weight=0.85"
})
class SubjectTagResolvedJpaRepositoryTest {

    @Autowired private TagResolutionService resolutionService;
    @Autowired private SubjectTagResolvedJpaRepository resolvedRepository;
    @Autowired private TagAssertionJpaRepository assertionRepository;
    @Autowired private TagJpaRepository tagRepository;
    @Autowired private TrackJpaRepository trackRepository;
    @Autowired private AlbumJpaRepository albumRepository;
    @Autowired private EntityManager entityManager;

    private TrackEntity track;

    @BeforeEach
    void setUp() {
        AlbumEntity album = albumRepository.saveAndFlush(AlbumEntity.create("Album", "resolution-album", 2026));
        track = trackRepository.saveAndFlush(TrackEntity.create(
                "Track", "resolution-track", "ISRC-R", 180_000, album
        ));
    }

    @Test
    void approved_direct_assertion을_max_score로_멱등_projection한다() {
        TagEntity tag = activeTag("ambient");
        assertionRepository.saveAndFlush(assertion(tag, AssertionSource.MUSICBRAINZ, 0.6, AssertionStatus.APPROVED));

        resolutionService.resolve(SubjectRef.track(track));
        assertionRepository.saveAndFlush(assertion(tag, AssertionSource.DISCOGS, 0.9, AssertionStatus.APPROVED));
        var result = resolutionService.resolve(SubjectRef.track(track));
        resolutionService.resolve(SubjectRef.track(track));

        assertThat(result).singleElement().satisfies(resolved -> {
            assertThat(resolved.tagId()).isEqualTo(tag.getTagId());
            assertThat(resolved.score()).isEqualTo(0.9);
            assertThat(resolved.reason()).isEqualTo(ResolutionReason.AUTO);
        });
        assertThat(resolvedRepository.count()).isEqualTo(1);
    }

    @Test
    void pending_rejected와_minimum_미만_assertion은_제외하고_obsolete_AUTO를_삭제한다() {
        TagEntity approved = activeTag("approved");
        TagEntity pending = activeTag("pending");
        TagEntity rejected = activeTag("rejected");
        TagEntity belowMinimum = activeTag("below-minimum");
        TagAssertionEntity approvedAssertion = assertionRepository.saveAndFlush(
                assertion(approved, AssertionSource.MUSICBRAINZ, 0.8, AssertionStatus.APPROVED)
        );
        assertionRepository.saveAllAndFlush(List.of(
                assertion(pending, AssertionSource.MUSICBRAINZ, 0.9, AssertionStatus.PENDING),
                assertion(rejected, AssertionSource.MUSICBRAINZ, 0.9, AssertionStatus.REJECTED),
                assertion(belowMinimum, AssertionSource.MUSICBRAINZ, 0.49, AssertionStatus.APPROVED)
        ));

        assertThat(resolutionService.resolve(SubjectRef.track(track)))
                .extracting(result -> result.tagId())
                .containsExactly(approved.getTagId());

        assertionRepository.delete(approvedAssertion);
        assertionRepository.flush();
        assertThat(resolutionService.resolve(SubjectRef.track(track))).isEmpty();
        assertThat(resolvedRepository.count()).isZero();
    }

    @Test
    void inherited_only_assertion은_album_inheritance_reason으로_projection한다() {
        TagEntity tag = activeTag("inherited-only");
        TagAssertionEntity albumAssertion = assertionRepository.saveAndFlush(
                TagAssertionEntity.createApproved(
                        SubjectRef.album(track.getAlbum()),
                        tag,
                        AssertionSource.MUSICBRAINZ,
                        EvidenceType.EXPLICIT_GENRE,
                        1.0
                )
        );
        entityManager.createNativeQuery("""
                insert into tag_assertion
                    (subject_type, subject_id, tag_id, source, evidence_type, confidence, status,
                     inherited_from_assertion_id, created_at)
                values ('TRACK', :subjectId, :tagId, 'MUSICBRAINZ', 'EXPLICIT_GENRE', 0.85,
                        'APPROVED', :parentId, current_timestamp)
                """)
                .setParameter("subjectId", track.getTrackId())
                .setParameter("tagId", tag.getTagId())
                .setParameter("parentId", albumAssertion.getAssertionId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertThat(resolutionService.resolve(SubjectRef.track(track.getTrackId())))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.tagId()).isEqualTo(tag.getTagId());
                    assertThat(result.score()).isEqualTo(0.85);
                    assertThat(result.reason()).isEqualTo(ResolutionReason.INHERITED_FROM_ALBUM);
                });
        assertThat(resolvedRepository.count()).isEqualTo(1);
    }

    @Test
    void Album의_approved_direct_assertion도_Album_projection으로_계산한다() {
        TagEntity tag = activeTag("album-direct");
        assertionRepository.saveAndFlush(TagAssertionEntity.createApproved(
                SubjectRef.album(track.getAlbum()),
                tag,
                AssertionSource.DISCOGS,
                EvidenceType.EXPLICIT_STYLE,
                0.75
        ));

        assertThat(resolutionService.resolve(SubjectRef.album(track.getAlbum())))
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.tagId()).isEqualTo(tag.getTagId());
                    assertThat(result.score()).isEqualTo(0.75);
                });
    }

    @Test
    void MANUAL_FIXED와_HIDDEN은_같은_AUTO_candidate가_있어도_보존한다() {
        TagEntity fixedTag = activeTag("fixed");
        TagEntity hiddenTag = activeTag("hidden");
        resolvedRepository.saveAllAndFlush(List.of(
                SubjectTagResolvedEntity.manual(
                        SubjectRef.track(track), fixedTag, 1.0, ResolvedStatus.MANUAL_FIXED,
                        ResolutionReason.ADMIN_APPROVED, LocalDateTime.now()
                ),
                SubjectTagResolvedEntity.manual(
                        SubjectRef.track(track), hiddenTag, 0.7, ResolvedStatus.HIDDEN,
                        ResolutionReason.ADMIN_APPROVED, LocalDateTime.now()
                )
        ));
        assertionRepository.saveAllAndFlush(List.of(
                assertion(fixedTag, AssertionSource.ADMIN, 0.6, AssertionStatus.APPROVED),
                assertion(hiddenTag, AssertionSource.ADMIN, 0.9, AssertionStatus.APPROVED)
        ));

        resolutionService.resolve(SubjectRef.track(track));
        entityManager.clear();

        assertThat(resolvedRepository.findAllBySubjectWithTag(SubjectType.TRACK, track.getTrackId()))
                .extracting(SubjectTagResolvedEntity::getStatus)
                .containsExactlyInAnyOrder(ResolvedStatus.MANUAL_FIXED, ResolvedStatus.HIDDEN);
    }

    @Test
    void detail_조회는_HIDDEN을_제외하고_score와_tagId로_정렬해_한_query로_Tag를_적재한다() {
        TagEntity first = activeTag("detail-first");
        TagEntity second = activeTag("detail-second");
        TagEntity lower = activeTag("detail-lower");
        TagEntity hidden = activeTag("detail-hidden");
        resolvedRepository.saveAllAndFlush(List.of(
                SubjectTagResolvedEntity.manual(
                        SubjectRef.track(track), second, 0.9, ResolvedStatus.MANUAL_FIXED,
                        ResolutionReason.ADMIN_APPROVED, LocalDateTime.now()
                ),
                SubjectTagResolvedEntity.manual(
                        SubjectRef.track(track), lower, 0.7, ResolvedStatus.MANUAL_FIXED,
                        ResolutionReason.ADMIN_APPROVED, LocalDateTime.now()
                ),
                SubjectTagResolvedEntity.manual(
                        SubjectRef.track(track), first, 0.9, ResolvedStatus.MANUAL_FIXED,
                        ResolutionReason.ADMIN_APPROVED, LocalDateTime.now()
                ),
                SubjectTagResolvedEntity.manual(
                        SubjectRef.track(track), hidden, 1.0, ResolvedStatus.HIDDEN,
                        ResolutionReason.ADMIN_APPROVED, LocalDateTime.now()
                )
        ));
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        var visible = resolvedRepository.findVisibleBySubjectWithTag(
                SubjectType.TRACK, track.getTrackId(), ResolvedStatus.HIDDEN
        );
        visible.forEach(row -> assertThat(row.getTag().getName()).isNotBlank());

        assertThat(visible)
                .extracting(row -> row.getTag().getTagId())
                .containsExactly(first.getTagId(), second.getTagId(), lower.getTagId());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1L);
    }

    @Test
    void MERGED는_최종_ACTIVE로_저장하고_cycle이면_기존_projection도_변경하지_않는다() {
        TagEntity canonical = activeTag("canonical");
        TagEntity merged = activeTag("merged");
        merged.mergeInto(canonical);
        tagRepository.saveAndFlush(merged);
        assertionRepository.saveAndFlush(assertion(
                merged, AssertionSource.MUSICBRAINZ, 0.8, AssertionStatus.APPROVED
        ));

        assertThat(resolutionService.resolve(SubjectRef.track(track)))
                .extracting(result -> result.tagId())
                .containsExactly(canonical.getTagId());

        TagEntity cycleA = activeTag("cycle-a");
        TagEntity cycleB = activeTag("cycle-b");
        cycleA.mergeInto(cycleB);
        cycleB.mergeInto(cycleA);
        tagRepository.saveAllAndFlush(List.of(cycleA, cycleB));
        assertionRepository.saveAndFlush(assertion(
                cycleA, AssertionSource.ADMIN, 0.9, AssertionStatus.APPROVED
        ));

        assertThatThrownBy(() -> resolutionService.resolve(SubjectRef.track(track)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
        assertThat(resolvedRepository.count()).isEqualTo(1);
    }

    @Test
    void bulk_조회_query수는_assertion과_resolved_row수에_비례하지_않는다() {
        for (int index = 0; index < 10; index++) {
            TagEntity tag = activeTag("bulk-" + index);
            assertionRepository.save(assertion(
                    tag, AssertionSource.MUSICBRAINZ, 0.8, AssertionStatus.APPROVED
            ));
        }
        assertionRepository.flush();
        resolutionService.resolve(SubjectRef.track(track));
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        resolutionService.resolve(SubjectRef.track(track));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(7L);
    }

    @Test
    void resolved_unique를_DB가_보호하고_다른_Subject는_변경하지_않는다() {
        TagEntity tag = activeTag("constraints");
        AlbumEntity otherAlbum = albumRepository.saveAndFlush(AlbumEntity.create(
                "Other", "resolution-other-album", 2025
        ));
        resolvedRepository.saveAndFlush(SubjectTagResolvedEntity.automatic(
                SubjectRef.album(otherAlbum), tag, 0.7, LocalDateTime.now()
        ));
        assertionRepository.saveAndFlush(assertion(
                tag, AssertionSource.MUSICBRAINZ, 0.8, AssertionStatus.APPROVED
        ));
        resolutionService.resolve(SubjectRef.track(track));

        assertThat(resolvedRepository.count()).isEqualTo(2);
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into subject_tag_resolved
                    (subject_type, subject_id, tag_id, score, status, resolution_reason, last_resolved_at)
                values ('TRACK', :subjectId, :tagId, 0.5, 'ACTIVE', 'AUTO', current_timestamp)
                """).setParameter("subjectId", track.getTrackId())
                .setParameter("tagId", tag.getTagId())
                .executeUpdate()).isInstanceOf(PersistenceException.class);
    }

    @Test
    void resolved_tag_FK를_DB가_보호한다() {
        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into subject_tag_resolved
                    (subject_type, subject_id, tag_id, score, status, resolution_reason, last_resolved_at)
                values ('TRACK', :subjectId, 999999, 0.5, 'ACTIVE', 'AUTO', current_timestamp)
                """).setParameter("subjectId", track.getTrackId())
                .executeUpdate()).isInstanceOf(PersistenceException.class);
    }

    private TagEntity activeTag(String slug) {
        return tagRepository.saveAndFlush(TagEntity.create(
                slug, slug, TagType.GENRE, TagStatus.ACTIVE, null
        ));
    }

    private TagAssertionEntity assertion(
            TagEntity tag,
            AssertionSource source,
            double confidence,
            AssertionStatus status
    ) {
        return TagAssertionEntity.create(
                SubjectRef.track(track), tag, source, EvidenceType.EXPLICIT_GENRE, confidence, status
        );
    }
}
