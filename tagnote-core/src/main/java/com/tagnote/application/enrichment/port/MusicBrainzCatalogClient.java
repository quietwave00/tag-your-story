package com.tagnote.application.enrichment.port;

import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingCandidate;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.RecordingDetails;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupDetails;
import com.tagnote.application.enrichment.matching.model.MusicBrainzCatalogData.ReleaseGroupCandidate;

import java.util.List;

public interface MusicBrainzCatalogClient {

    List<RecordingCandidate> searchByIsrc(String isrc);

    List<RecordingCandidate> searchByTitleAndArtists(String title, List<String> artists);

    RecordingDetails getRecording(String recordingId);

    RecordingDetails getRecordingReleaseGroups(String recordingId);

    List<ReleaseGroupCandidate> searchReleaseGroups(String title, List<String> artists);

    ReleaseGroupDetails getReleaseGroup(String releaseGroupId);
}
