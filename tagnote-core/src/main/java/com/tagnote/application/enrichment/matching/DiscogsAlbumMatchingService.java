package com.tagnote.application.enrichment.matching;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
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
        List<AlbumCandidate> artistMatches = candidates.stream()
                .filter(candidate -> artistOverlap(album.getArtists(), candidate.artistNames()))
                .toList();
        List<AlbumCandidate> rawTitleMatches = artistMatches.stream()
                .filter(candidate -> identityNormalizer.rawTitleExact(album.getTitle(), candidate.title()))
                .toList();
        List<AlbumCandidate> titleMatches = rawTitleMatches.isEmpty()
                ? artistMatches.stream()
                        .filter(candidate -> identityNormalizer.canonicalTitleExact(
                                album.getTitle(), candidate.title()
                        ))
                        .toList()
                : rawTitleMatches;
        return preferMatchingYear(album.getReleaseYear(), titleMatches);
    }

    public boolean validates(ImportedAlbum album, AlbumDetails details) {
        return identityNormalizer.canonicalTitleExact(album.getTitle(), details.title())
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
