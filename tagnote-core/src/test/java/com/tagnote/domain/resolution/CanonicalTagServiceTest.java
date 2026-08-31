package com.tagnote.domain.resolution;

import com.tagnote.domain.taxonomy.tag.TagStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CanonicalTagServiceTest {

    private final CanonicalTagService service = new CanonicalTagService();

    @Test
    void ACTIVE는_자기_자신을_canonical_tag로_반환한다() {
        var result = service.findActiveCanonicalTag(1L, Map.of(
                1L, node(1L, TagStatus.ACTIVE, null)
        ));

        assertThat(result).hasValue(1L);
    }

    @Test
    void MERGED_chain은_최종_ACTIVE_tag까지_따라간다() {
        var result = service.findActiveCanonicalTag(1L, Map.of(
                1L, node(1L, TagStatus.MERGED, 2L),
                2L, node(2L, TagStatus.MERGED, 3L),
                3L, node(3L, TagStatus.ACTIVE, null)
        ));

        assertThat(result).hasValue(3L);
    }

    @Test
    void 비노출_상태와_유효하지_않은_merge_target은_제외한다() {
        Map<Long, CanonicalTagNode> taxonomy = Map.of(
                1L, node(1L, TagStatus.CANDIDATE, null),
                2L, node(2L, TagStatus.DEPRECATED, null),
                3L, node(3L, TagStatus.MERGED, 99L)
        );

        assertThat(service.findActiveCanonicalTag(1L, taxonomy)).isEmpty();
        assertThat(service.findActiveCanonicalTag(2L, taxonomy)).isEmpty();
        assertThat(service.findActiveCanonicalTag(3L, taxonomy)).isEmpty();
    }

    @Test
    void canonical_chain_cycle은_실패시킨다() {
        Map<Long, CanonicalTagNode> taxonomy = Map.of(
                1L, node(1L, TagStatus.MERGED, 2L),
                2L, node(2L, TagStatus.MERGED, 1L)
        );

        assertThatThrownBy(() -> service.findActiveCanonicalTag(1L, taxonomy))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cycle");
    }

    private CanonicalTagNode node(long id, TagStatus status, Long targetId) {
        return new CanonicalTagNode(id, status, targetId);
    }
}
