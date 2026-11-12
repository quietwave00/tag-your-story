package com.tagnote.application.enrichment.matching;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.enrichment.diagnostics.EnrichmentDiagnostics;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumCandidate;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscogsAlbumMatchingService {

    private final DiscogsAlbumIdentityNormalizer identityNormalizer;

    public String searchTitle(ImportedAlbum album) {
        return identityNormalizer.searchTitle(album.getTitle());
    }

    public List<AlbumCandidate> matchingCandidates(ImportedAlbum album, List<AlbumCandidate> candidates) {
        return matchingCandidates(album, album.getTitle(), candidates);
    }

    public List<AlbumCandidate> matchingCandidates(
            ImportedAlbum album,
            String validationTitle,
            List<AlbumCandidate> candidates
    ) {
        List<AlbumCandidate> artistMatches = candidates.stream()
                .filter(candidate -> artistOverlap(album.getArtists(), candidate.artistNames()))
                .toList();
        List<AlbumCandidate> rawTitleMatches = artistMatches.stream()
                .filter(candidate -> identityNormalizer.rawTitleExact(validationTitle, candidate.title()))
                .toList();
        List<AlbumCandidate> titleMatches = rawTitleMatches.isEmpty()
                ? artistMatches.stream()
                        .filter(candidate -> identityNormalizer.canonicalTitleExact(
                                validationTitle, candidate.title()
                        ))
                        .toList()
                : rawTitleMatches;
        List<AlbumCandidate> yearMatches = preferMatchingYear(album.getReleaseYear(), titleMatches);
        if (EnrichmentDiagnostics.enabled()) {
            EnrichmentDiagnostics.event("discogs_candidates_filtered", java.util.Map.of(
                    "validationTitle", EnrichmentDiagnostics.safe(validationTitle),
                    "rawCount", candidates.size(), "artistCount", artistMatches.size(),
                    "rawTitleCount", rawTitleMatches.size(), "titleCount", titleMatches.size(),
                    "yearCount", yearMatches.size(),
                    "candidates", EnrichmentDiagnostics.bounded(candidates.stream().map(candidate ->
                            java.util.Map.of("id", candidate.id(), "type", candidate.type().name(),
                                    "title", EnrichmentDiagnostics.safe(candidate.title()), "year", candidate.releaseYear() == null
                                            ? "UNKNOWN" : candidate.releaseYear(),
                                    "decision", !artistMatches.contains(candidate) ? "ARTIST_MISMATCH"
                                            : !titleMatches.contains(candidate) ? "TITLE_MISMATCH"
                                            : !yearMatches.contains(candidate) ? "YEAR_PREFERENCE" : "MATCHED"))
                            .toList())));
        }
        return yearMatches;
    }

    public boolean validates(ImportedAlbum album, AlbumDetails details) {
        return validates(album, album.getTitle(), details);
    }

    public boolean validates(ImportedAlbum album, String validationTitle, AlbumDetails details) {
        return identityNormalizer.canonicalTitleExact(validationTitle, details.title())
                && artistOverlap(album.getArtists(), details.artistNames());
    }

    private boolean artistOverlap(List<ImportedArtist> imported, List<String> candidates) {
        return imported.stream().map(ImportedArtist::getName)
                .anyMatch(name -> candidates.stream()
                        .anyMatch(candidate -> identityNormalizer.artistExact(name, candidate)));
    }

    private List<AlbumCandidate> preferMatchingYear(Integer expected, List<AlbumCandidate> candidates) {
        if (expected == null || candidates.size() <= 1) {
            return candidates;
        }
        List<AlbumCandidate> sameYear = candidates.stream()
                .filter(candidate -> expected.equals(candidate.releaseYear()))
                .toList();
        return sameYear.isEmpty() ? candidates : sameYear;
    }
}
