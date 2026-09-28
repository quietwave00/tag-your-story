package com.tagnote.application.enrichment;

import com.tagnote.application.enrichment.model.CollectedExternalTags;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.matching.TagMatchResult;
import com.tagnote.domain.taxonomy.matching.TagMatchingService;
import com.tagnote.domain.taxonomy.matching.TagNameNormalizer;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.domain.taxonomy.tag.TagStatus;
import com.tagnote.infrastructure.persistence.taxonomy.TagAliasJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TagBootstrapService {
    private final TagNameNormalizer normalizer;
    private final TagMatchingService matching;
    private final TagAliasJpaRepository aliases;
    private final TagJpaRepository tags;
    private final TagBootstrapWriteService writer;

    public Map<String, TagMatchResult> prepare(CollectedExternalTags collected) {
        Map<String, NameCandidate> names = new LinkedHashMap<>();
        Stream.concat(collected.albumInputs().stream(), collected.trackInputs().stream())
                .sorted((a, b) -> Integer.compare(priority(a), priority(b)))
                .forEach(input -> {
                    String name = normalizer.normalize(input.rawName()).value();
                    if (name.length() <= 255 && input.rawName().length() <= 255) {
                        names.putIfAbsent(name, new NameCandidate(name, input.rawName(), eligible(input)));
                        if (eligible(input) && !names.get(name).eligible()) {
                            names.put(name, new NameCandidate(name, input.rawName(), true));
                        }
                    }
                });
        if (names.isEmpty()) return Map.of();
        Set<String> keys = names.keySet();
        Map<String, List<TagAliasEntity>> aliasesByName = aliases.findApprovedByNormalizedAliases(keys)
                .stream().collect(Collectors.groupingBy(TagAliasEntity::getNormalizedAlias));
        Map<String, TagEntity> canonical = tags.findByNormalizedNameIn(keys).stream()
                .collect(Collectors.toMap(TagEntity::getNormalizedName, tag -> tag));
        Map<String, TagMatchResult> result = new HashMap<>();
        List<NameCandidate> missing = new ArrayList<>();
        for (NameCandidate candidate : names.values()) {
            String name = candidate.normalizedName();
            TagMatchResult alias = matching.match(normalizer.normalize(name),
                    aliasesByName.getOrDefault(name, List.of()));
            if (alias.getStatus() != TagMatchResult.Status.UNMATCHED) {
                result.put(name, alias);
            } else if (canonical.containsKey(name)) {
                TagEntity tag = canonical.get(name);
                result.put(name, tag.getStatus() == TagStatus.ACTIVE
                        ? TagMatchResult.matched(tag) : TagMatchResult.unmatched());
            } else if (candidate.eligible()) {
                missing.add(candidate);
            }
        }
        if (!missing.isEmpty()) {
            List<TagEntity> created;
            try {
                created = writer.create(missing);
            } catch (TagBootstrapWriteService.TagDuplicateException conflict) {
                // The failed transaction has rolled back. Re-read every requested name once.
                created = new ArrayList<>(tags.findByNormalizedNameIn(
                        missing.stream().map(NameCandidate::normalizedName).toList()));
                Set<String> existingNames = created.stream().map(TagEntity::getNormalizedName)
                        .collect(Collectors.toSet());
                List<NameCandidate> remaining = missing.stream()
                        .filter(candidate -> !existingNames.contains(candidate.normalizedName())).toList();
                if (!remaining.isEmpty()) created.addAll(writer.create(remaining));
            }
            for (TagEntity tag : created) {
                result.put(tag.getNormalizedName(), tag.getStatus() == TagStatus.ACTIVE
                        ? TagMatchResult.matched(tag) : TagMatchResult.unmatched());
            }
        }
        return Map.copyOf(result);
    }

    private int priority(ExternalTagInput input) {
        return switch (input.source()) {
            case MUSICBRAINZ -> 0;
            case DISCOGS -> 1;
            case LASTFM -> 2;
        };
    }

    private boolean eligible(ExternalTagInput input) {
        return (input.source() == ExternalTagSource.MUSICBRAINZ
                && input.evidenceType() == EvidenceType.EXPLICIT_GENRE)
                || (input.source() == ExternalTagSource.DISCOGS
                && (input.evidenceType() == EvidenceType.EXPLICIT_GENRE
                || input.evidenceType() == EvidenceType.EXPLICIT_STYLE));
    }

    public record NameCandidate(String normalizedName, String displayName, boolean eligible) {}
}
