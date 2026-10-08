package com.tagnote.domain.taxonomy.tag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagEntityTest {

    @Test
    void merge하면_MERGED_상태와_대상을_함께_설정한다() {
        TagEntity source = tag("idm", TagStatus.ACTIVE);
        TagEntity target = tag("intelligent-dance-music", TagStatus.ACTIVE);

        source.mergeInto(target);

        assertThat(source.getStatus()).isEqualTo(TagStatus.MERGED);
        assertThat(source.getMergedIntoTag()).isSameAs(target);
    }

    @Test
    void MERGED_상태는_mergeInto를_통해서만_만든다() {
        assertThatThrownBy(() -> tag("idm", TagStatus.MERGED))
                .isInstanceOf(IllegalArgumentException.class);

        TagEntity tag = tag("idm", TagStatus.ACTIVE);
        assertThatThrownBy(() -> tag.changeStatus(TagStatus.MERGED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 자기_자신으로_merge할_수_없다() {
        TagEntity tag = tag("idm", TagStatus.ACTIVE);

        assertThatThrownBy(() -> tag.mergeInto(tag))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void MERGED가_아닌_상태로_변경하면_merge_대상을_제거한다() {
        TagEntity source = tag("idm", TagStatus.ACTIVE);
        source.mergeInto(tag("intelligent-dance-music", TagStatus.ACTIVE));

        source.changeStatus(TagStatus.DEPRECATED);

        assertThat(source.getStatus()).isEqualTo(TagStatus.DEPRECATED);
        assertThat(source.getMergedIntoTag()).isNull();
    }

    private TagEntity tag(String slug, TagStatus status) {
        return TagEntity.create(slug, slug, TagType.GENRE, status, null);
    }
}
