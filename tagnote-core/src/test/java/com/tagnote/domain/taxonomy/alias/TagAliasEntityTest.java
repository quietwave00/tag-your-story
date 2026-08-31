package com.tagnote.domain.taxonomy.alias;

import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TagAliasEntityTest {

    private final TagNameNormalizer normalizer = new TagNameNormalizer();

    @Test
    void 원본과_normalized_alias를_분리해_보존한다() {
        TagAliasEntity alias = pendingAlias("  Drum 'n' Bass  ");

        assertThat(alias.getAlias()).isEqualTo("  Drum 'n' Bass  ");
        assertThat(alias.getNormalizedAlias()).isEqualTo("drum 'n' bass");
    }

    @Test
    void pending_alias를_승인하거나_거절한다() {
        TagAliasEntity approved = pendingAlias("IDM");
        TagAliasEntity rejected = pendingAlias("Wrong");

        approved.approve();
        rejected.reject();

        assertThat(approved.getStatus()).isEqualTo(AliasStatus.APPROVED);
        assertThat(rejected.getStatus()).isEqualTo(AliasStatus.REJECTED);
    }

    @Test
    void 검토가_끝난_alias의_상태를_다시_변경할_수_없다() {
        TagAliasEntity alias = pendingAlias("IDM");
        alias.approve();

        assertThatThrownBy(alias::reject).isInstanceOf(IllegalStateException.class);
    }

    private TagAliasEntity pendingAlias(String rawAlias) {
        TagEntity tag = TagEntity.create("IDM", "idm", TagType.GENRE, TagStatus.ACTIVE, null);
        return TagAliasEntity.create(
                tag,
                rawAlias,
                normalizer.normalize(rawAlias),
                AliasSource.ADMIN,
                AliasStatus.PENDING
        );
    }
}
