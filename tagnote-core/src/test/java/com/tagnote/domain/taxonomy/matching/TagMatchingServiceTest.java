package com.tagnote.domain.taxonomy.matching;

import com.tagnote.domain.taxonomy.alias.AliasSource;
import com.tagnote.domain.taxonomy.alias.AliasStatus;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TagMatchingServiceTest {

    private final TagNameNormalizer normalizer = new TagNameNormalizer();
    private final TagMatchingService matchingService = new TagMatchingService();

    @Test
    void approved_exact_alias가_하나면_Tag를_반환한다() {
        TagEntity tag = tag("ambient");
        NormalizedTagName name = normalizer.normalize("Ambient");

        TagMatchResult result = matchingService.match(name, List.of(alias(tag, "ambient", AliasStatus.APPROVED)));

        assertThat(result.getStatus()).isEqualTo(TagMatchResult.Status.MATCHED);
        assertThat(result.getMatchedTag()).containsSame(tag);
    }

    @Test
    void 후보가_없거나_pending이면_unmatched다() {
        NormalizedTagName name = normalizer.normalize("Ambient");

        assertThat(matchingService.match(name, List.of()).getStatus())
                .isEqualTo(TagMatchResult.Status.UNMATCHED);
        assertThat(matchingService.match(name, List.of(alias(tag("ambient"), "ambient", AliasStatus.PENDING))).getStatus())
                .isEqualTo(TagMatchResult.Status.UNMATCHED);
    }

    @Test
    void 다른_normalized_alias는_approved여도_unmatched다() {
        TagMatchResult result = matchingService.match(
                normalizer.normalize("ambient"),
                List.of(alias(tag("house"), "house", AliasStatus.APPROVED))
        );

        assertThat(result.getStatus()).isEqualTo(TagMatchResult.Status.UNMATCHED);
    }

    @Test
    void 같은_alias가_복수_Tag를_가리키면_ambiguous다() {
        NormalizedTagName name = normalizer.normalize("garage");

        TagMatchResult result = matchingService.match(name, List.of(
                alias(tag("garage-rock"), "garage", AliasStatus.APPROVED),
                alias(tag("uk-garage"), "garage", AliasStatus.APPROVED)
        ));

        assertThat(result.getStatus()).isEqualTo(TagMatchResult.Status.AMBIGUOUS);
        assertThat(result.getMatchedTag()).isEmpty();
    }

    @Test
    void 같은_Tag의_중복_후보는_하나로_판정한다() {
        TagEntity tag = tag("idm");
        NormalizedTagName name = normalizer.normalize("IDM");

        TagMatchResult result = matchingService.match(name, List.of(
                alias(tag, "IDM", AliasStatus.APPROVED),
                alias(tag, "idm", AliasStatus.APPROVED)
        ));

        assertThat(result.getStatus()).isEqualTo(TagMatchResult.Status.MATCHED);
        assertThat(result.getMatchedTag()).containsSame(tag);
    }

    private TagEntity tag(String slug) {
        return TagEntity.create(slug, slug, TagType.GENRE, TagStatus.ACTIVE, null);
    }

    private TagAliasEntity alias(TagEntity tag, String rawAlias, AliasStatus status) {
        return TagAliasEntity.create(tag, rawAlias, normalizer.normalize(rawAlias), AliasSource.ADMIN, status);
    }
}
