package com.tagnote.domain.resolution;

import com.tagnote.domain.taxonomy.tag.TagStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TagResolverTest {

    private final TagResolver resolver = new TagResolver(new CanonicalTagService());

    @Test
    void 같은_canonical_tag의_score는_max이고_minimum_경계값은_포함한다() {
        Map<Long, CanonicalTagNode> taxonomy = Map.of(
                1L, node(1L, TagStatus.ACTIVE, null),
                2L, node(2L, TagStatus.MERGED, 1L),
                3L, node(3L, TagStatus.ACTIVE, null),
                4L, node(4L, TagStatus.ACTIVE, null)
        );

        List<ResolvedTagCandidate> result = resolver.resolve(List.of(
                new DirectTagEvidence(1L, 0.7),
                new DirectTagEvidence(2L, 0.9),
                new DirectTagEvidence(3L, 0.5),
                new DirectTagEvidence(4L, 0.49)
        ), taxonomy, 0.5);

        assertThat(result).containsExactly(
                ResolvedTagCandidate.automatic(1L, 0.9),
                ResolvedTagCandidate.automatic(3L, 0.5)
        );
    }

    @Test
    void 입력_순서가_달라도_결과와_score는_같고_반복_계산해도_drift가_없다() {
        Map<Long, CanonicalTagNode> taxonomy = Map.of(
                1L, node(1L, TagStatus.ACTIVE, null),
                2L, node(2L, TagStatus.ACTIVE, null)
        );
        List<DirectTagEvidence> firstOrder = List.of(
                new DirectTagEvidence(2L, 0.8), new DirectTagEvidence(1L, 0.6)
        );
        List<DirectTagEvidence> reverseOrder = List.of(
                new DirectTagEvidence(1L, 0.6), new DirectTagEvidence(2L, 0.8)
        );

        assertThat(resolver.resolve(firstOrder, taxonomy, 0.5))
                .isEqualTo(resolver.resolve(reverseOrder, taxonomy, 0.5))
                .isEqualTo(resolver.resolve(firstOrder, taxonomy, 0.5));
    }

    private CanonicalTagNode node(long id, TagStatus status, Long targetId) {
        return new CanonicalTagNode(id, status, targetId);
    }
}
