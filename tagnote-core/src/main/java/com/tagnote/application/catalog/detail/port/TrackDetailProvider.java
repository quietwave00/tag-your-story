package com.tagnote.application.catalog.detail.port;

import com.tagnote.core.domain.tracks.service.dto.TrackData;

public interface TrackDetailProvider {
    TrackData getTrack(String spotifyTrackId);
}
