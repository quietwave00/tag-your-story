package com.tagnote.application.enrichment.matching;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.config.ExternalEnrichmentProperties;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MusicBrainzEntityMatchingService {

    private final MusicEntityNameNormalizer normalizer;
    private final ExternalEnrichmentProperties properties;

    public Optional<RecordingCandidate> matchRecordingByIsrc(
            ImportedTrack track,
            List<RecordingCandidate> candidates
    ) {
        if (candidates.size() == 1) {
            RecordingCandidate candidate = candidates.get(0);
            return hasArtistOverlap(track.getArtists(), candidate.artistNames())
                    ? Optional.of(candidate)
                    : Optional.empty();
        }
        List<RecordingCandidate> matched = matchingRecordings(track, candidates);
        if (matched.size() <= 1 || track.getDurationMs() == null) {
            return unique(matched);
        }
        long minimumDifference = matched.stream()
                .mapToLong(candidate -> durationDifference(track.getDurationMs(), candidate.durationMs()))
                .min()
                .orElseThrow();
        return unique(matched.stream()
                .filter(candidate -> durationDifference(track.getDurationMs(), candidate.durationMs())
                        == minimumDifference)
                .toList());
    }

    public Optional<RecordingCandidate> matchRecordingByMetadata(
            ImportedTrack track,
            List<RecordingCandidate> candidates
    ) {
        return unique(matchingRecordings(track, candidates));
    }

    private List<RecordingCandidate> matchingRecordings(
            ImportedTrack track,
            List<RecordingCandidate> candidates
    ) {
        return candidates.stream()
                .filter(candidate -> normalizer.exact(track.getTitle(), candidate.title()))
                .filter(candidate -> hasArtistOverlap(track.getArtists(), candidate.artistNames()))
                .filter(candidate -> durationMatches(track.getDurationMs(), candidate.durationMs()))
                .toList();
    }

    public Optional<ReleaseGroupCandidate> matchReleaseGroup(
            ImportedAlbum album,
            List<ReleaseGroupCandidate> candidates
    ) {
        return unique(matchingReleaseGroups(album, album.getTitle(), candidates));
    }

    public List<ReleaseGroupCandidate> matchingReleaseGroups(
            ImportedAlbum album,
            String expectedTitle,
            List<ReleaseGroupCandidate> candidates
    ) {
        List<ReleaseGroupCandidate> distinct = List.copyOf(candidates.stream().collect(
                java.util.stream.Collectors.toMap(
                        ReleaseGroupCandidate::id,
                        candidate -> candidate,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                )
        ).values());
        return distinct.stream()
                .filter(candidate -> normalizer.exact(expectedTitle, candidate.title()))
                .filter(candidate -> hasArtistOverlap(album.getArtists(), candidate.artistNames()))
                .filter(candidate -> yearsMatch(album.getReleaseYear(), candidate.releaseYear()))
                .toList();
    }

    private boolean hasArtistOverlap(List<ImportedArtist> imported, List<String> candidates) {
        return imported.stream().map(ImportedArtist::getName)
                .anyMatch(name -> candidates.stream().anyMatch(candidate -> normalizer.exact(name, candidate)));
    }

    private boolean durationMatches(Integer expected, Integer actual) {
        return expected != null && actual != null
                && durationDifference(expected, actual) <= properties.getMatching().getDurationToleranceMs();
    }

    private long durationDifference(Integer expected, Integer actual) {
        return Math.abs((long) expected - actual);
    }

    private boolean yearsMatch(Integer expected, Integer actual) {
        return expected == null || actual == null || expected.equals(actual);
    }

    private <T> Optional<T> unique(List<T> matched) {
        return matched.size() == 1 ? Optional.of(matched.get(0)) : Optional.empty();
    }
}
