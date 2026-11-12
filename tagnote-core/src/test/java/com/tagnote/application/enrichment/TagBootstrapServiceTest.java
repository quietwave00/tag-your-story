package com.tagnote.application.enrichment;

import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.taxonomy.alias.AliasSource;
import com.tagnote.domain.taxonomy.alias.AliasStatus;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.matching.TagMatchingService;
import com.tagnote.domain.taxonomy.matching.TagMatchResult;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.domain.taxonomy.tag.TagType;
import com.tagnote.infrastructure.persistence.taxonomy.TagAliasJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagBootstrapServiceTest {

    private final TagNameNormalizer normalizer = new TagNameNormalizer();
    private final TagAliasJpaRepository aliases = mock(TagAliasJpaRepository.class);
    private final TagJpaRepository tags = mock(TagJpaRepository.class);
    private final TagBootstrapWriteService writer = mock(TagBootstrapWriteService.class);
    private final TagBootstrapService service = new TagBootstrapService(
            normalizer, new TagMatchingService(), aliases, tags, writer);

    @Test
    void Lastfm_단독_미매칭_community_tag를_active_unclassified_Tag로_생성한다() {
        TagEntity created = TagEntity.create("Dream pop", "auto-dream-pop",
                TagType.UNCLASSIFIED, TagStatus.ACTIVE, null);
        when(aliases.findApprovedByNormalizedAliases(anyCollection())).thenReturn(List.of());
        when(tags.findByNormalizedNameIn(anyCollection())).thenReturn(List.of());
        when(writer.create(anyList())).thenReturn(List.of(created));

        Map<String, TagMatchResult> result = service.prepare(new CollectedExternalTags(List.of(),
                List.of(lastfm("Dream pop"), lastfm("dream pop"))));

        assertThat(result.get("dream pop").getMatchedTag()).contains(created);
        verify(writer).create(org.mockito.ArgumentMatchers.argThat(candidates ->
                candidates.size() == 1
                        && candidates.get(0).displayName().equals("Dream pop")
                        && candidates.get(0).source() == ExternalTagSource.LASTFM));
    }

    @Test
    void Lastfm_alias가_모호하면_새_Tag를_만들지_않는다() {
        TagEntity first = TagEntity.create("First", "first", TagType.UNCLASSIFIED, TagStatus.ACTIVE, null);
        TagEntity second = TagEntity.create("Second", "second", TagType.UNCLASSIFIED, TagStatus.ACTIVE, null);
        when(aliases.findApprovedByNormalizedAliases(anyCollection())).thenReturn(List.of(
                TagAliasEntity.create(first, "Dream pop", normalizer.normalize("Dream pop"),
                        AliasSource.ADMIN, AliasStatus.APPROVED),
                TagAliasEntity.create(second, "Dream pop", normalizer.normalize("Dream pop"),
                        AliasSource.ADMIN, AliasStatus.APPROVED)));
        when(tags.findByNormalizedNameIn(anyCollection())).thenReturn(List.of());

        Map<String, TagMatchResult> result = service.prepare(new CollectedExternalTags(List.of(),
                List.of(lastfm("Dream pop"))));

        assertThat(result.get("dream pop").getStatus()).isEqualTo(TagMatchResult.Status.AMBIGUOUS);
        verify(writer, never()).create(anyList());
    }

    @Test
    void Lastfm_이름이_비활성_canonical_Tag와_충돌하면_생성하지_않는다() {
        TagEntity inactive = TagEntity.create("Dream pop", "inactive", TagType.UNCLASSIFIED,
                TagStatus.DEPRECATED, null);
        when(aliases.findApprovedByNormalizedAliases(anyCollection())).thenReturn(List.of());
        when(tags.findByNormalizedNameIn(anyCollection())).thenReturn(List.of(inactive));

        Map<String, TagMatchResult> result = service.prepare(new CollectedExternalTags(List.of(),
                List.of(lastfm("Dream pop"))));

        assertThat(result.get("dream pop").getStatus()).isEqualTo(TagMatchResult.Status.UNMATCHED);
        verify(writer, never()).create(anyList());
    }

    private ExternalTagInput lastfm(String name) {
        return new ExternalTagInput(ExternalTagSource.LASTFM, name, "lastfm:track:test",
                EvidenceType.COMMUNITY_TAG, 0.65);
    }
}
