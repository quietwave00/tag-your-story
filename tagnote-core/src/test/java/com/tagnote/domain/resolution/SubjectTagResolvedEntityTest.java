package com.tagnote.domain.resolution;

import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SubjectTagResolvedEntityTest {

    @Test
    void automatic_projection은_score와_시간만_자동_갱신할_수_있다() {
        LocalDateTime first = LocalDateTime.of(2026, 8, 31, 10, 0);
        LocalDateTime second = first.plusMinutes(1);
        SubjectTagResolvedEntity row = SubjectTagResolvedEntity.automatic(
                SubjectRef.track(1L), mock(TagEntity.class), 0.6, first
        );

        row.updateAutomatic(0.9, ResolutionReason.AUTO, second);

        assertThat(row.getScore()).isEqualTo(0.9);
        assertThat(row.getResolutionReason()).isEqualTo(ResolutionReason.AUTO);
        assertThat(row.getLastResolvedAt()).isEqualTo(second);
        assertThat(row.isAutomatic()).isTrue();
    }

    @Test
    void inherited_projection도_auto_managed로_갱신과_cleanup_대상이_된다() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 10, 0);
        SubjectTagResolvedEntity row = SubjectTagResolvedEntity.autoManaged(
                SubjectRef.track(1L),
                mock(TagEntity.class),
                0.68,
                ResolutionReason.INHERITED_FROM_ALBUM,
                now
        );

        row.updateAutomatic(0.9, ResolutionReason.AUTO, now.plusMinutes(1));

        assertThat(row.isAutomatic()).isTrue();
        assertThat(row.getScore()).isEqualTo(0.9);
        assertThat(row.getResolutionReason()).isEqualTo(ResolutionReason.AUTO);
    }

    @Test
    void manual과_hidden_projection은_자동_갱신을_거부한다() {
        SubjectTagResolvedEntity manual = SubjectTagResolvedEntity.manual(
                SubjectRef.album(1L),
                mock(TagEntity.class),
                1.0,
                ResolvedStatus.MANUAL_FIXED,
                ResolutionReason.ADMIN_APPROVED,
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> manual.updateAutomatic(0.5, ResolutionReason.AUTO, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(manual.isAutomatic()).isFalse();
    }
}
