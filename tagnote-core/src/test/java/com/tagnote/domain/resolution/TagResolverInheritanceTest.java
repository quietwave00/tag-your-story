package com.tagnote.domain.resolution;

import com.tagnote.domain.taxonomy.tag.TagStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TagResolverInheritanceTest {

    private final TagResolver resolver = new TagResolver(new CanonicalTagService());

    @Test
    void direct_evidence가_같은_canonical_tag의_inherited보다_우선한다() {
        Map<Long, CanonicalTagNode> taxonomy = Map.of(
                1L, node(1L, TagStatus.ACTIVE, null),
                2L, node(2L, TagStatus.MERGED, 1L)
        );

        List<ResolvedTagCandidate> result = resolver.resolve(List.of(
                DirectTagEvidence.inheritedFromAlbum(1L, 0.95),
                new DirectTagEvidence(2L, 0.60)
        ), taxonomy, 0.5);

        assertThat(result).containsExactly(ResolvedTagCandidate.automatic(1L, 0.60));
    }

    @Test
    void direct가_없으면_inherited를_선택하고_inherited_reason을_사용한다() {
        Map<Long, CanonicalTagNode> taxonomy = Map.of(1L, node(1L, TagStatus.ACTIVE, null));

        List<ResolvedTagCandidate> result = resolver.resolve(List.of(
                DirectTagEvidence.inheritedFromAlbum(1L, 0.68),
                DirectTagEvidence.inheritedFromAlbum(1L, 0.70)
        ), taxonomy, 0.5);

        assertThat(result).containsExactly(ResolvedTagCandidate.inheritedFromAlbum(1L, 0.70));
    }

    @Test
    void 입력_순서가_달라도_direct와_inherited_우선순위는_같다() {
        Map<Long, CanonicalTagNode> taxonomy = Map.of(1L, node(1L, TagStatus.ACTIVE, null));
        List<DirectTagEvidence> directFirst = List.of(
                new DirectTagEvidence(1L, 0.60),
                DirectTagEvidence.inheritedFromAlbum(1L, 0.95)
        );
        List<DirectTagEvidence> inheritedFirst = List.of(
                DirectTagEvidence.inheritedFromAlbum(1L, 0.95),
                new DirectTagEvidence(1L, 0.60)
        );

        assertThat(resolver.resolve(directFirst, taxonomy, 0.5))
                .isEqualTo(resolver.resolve(inheritedFirst, taxonomy, 0.5));
    }

    private CanonicalTagNode node(long id, TagStatus status, Long targetId) {
        return new CanonicalTagNode(id, status, targetId);
    }
}
