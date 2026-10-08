package com.tagnote.application.enrichment.matching;

import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedArtist;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumSearchAttempt;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.AlbumSearchQuery;
import com.tagnote.application.enrichment.matching.model.DiscogsCatalogData.SearchField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DiscogsAlbumSearchPlan {

    private final DiscogsAlbumIdentityNormalizer identityNormalizer;

    public List<AlbumSearchAttempt> attempts(ImportedTrack track) {
        ImportedAlbum album = track.getAlbum();
        List<AlbumSearchAttempt> attempts = new ArrayList<>(attempts(album));
        List<String> albumArtists = album.getArtists().stream()
                .map(ImportedArtist::getName)
                .toList();
        List<String> trackArtists = track.getArtists().stream()
                .map(ImportedArtist::getName)
                .toList();
        String fallbackTitle = identityNormalizer.boxSetBaseTitle(
                identityNormalizer.searchTitle(album.getTitle())).orElse(null);
        if (track.getTitle() != null && !track.getTitle().isBlank()) {
            attempts.add(attempt(
                    SearchField.TRACK,
                    track.getTitle(),
                    trackArtists.isEmpty() ? albumArtists : trackArtists,
                    fallbackTitle == null ? album.getTitle() : fallbackTitle
            ));
        }
        return List.copyOf(attempts);
    }

    public List<AlbumSearchAttempt> attempts(ImportedAlbum album) {
        List<String> albumArtists = album.getArtists().stream()
                .map(ImportedArtist::getName)
                .toList();
        String canonicalTitle = identityNormalizer.searchTitle(album.getTitle());

        List<AlbumSearchAttempt> attempts = new ArrayList<>();
        attempts.add(attempt(SearchField.RELEASE_TITLE, canonicalTitle, albumArtists, album.getTitle()));

        String fallbackTitle = identityNormalizer.boxSetBaseTitle(canonicalTitle).orElse(null);
        if (fallbackTitle != null) {
            attempts.add(attempt(SearchField.RELEASE_TITLE, fallbackTitle, albumArtists, fallbackTitle));
        }
        return List.copyOf(attempts);
    }

    private AlbumSearchAttempt attempt(
            SearchField field,
            String value,
            List<String> artists,
            String validationTitle
    ) {
        return new AlbumSearchAttempt(new AlbumSearchQuery(field, value, artists), validationTitle);
    }
}
