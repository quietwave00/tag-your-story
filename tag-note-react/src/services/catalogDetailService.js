import { apiClient } from './apiClient.js';

export const catalogDetailService = {
  getArtist(spotifyArtistId, signal) {
    return apiClient.get(`/api/catalog/artists/${encodeURIComponent(spotifyArtistId)}`, { signal });
  },
  getArtistAlbums(spotifyArtistId, page, releaseType, signal) {
    return apiClient.get(`/api/catalog/artists/${encodeURIComponent(spotifyArtistId)}/albums`, {
      params: { page: page - 1, releaseType }, signal,
    });
  },
  getAlbum(spotifyAlbumId, signal) {
    return apiClient.get(`/api/catalog/albums/${encodeURIComponent(spotifyAlbumId)}`, { signal });
  },
  getAlbumTracks(spotifyAlbumId, page, signal) {
    return apiClient.get(`/api/catalog/albums/${encodeURIComponent(spotifyAlbumId)}/tracks`, {
      params: { page: page - 1 }, signal,
    });
  },
  async getAllAlbumTracks(spotifyAlbumId, signal) {
    const items = [];
    let page = 1;
    let totalCount = Infinity;

    while (items.length < totalCount) {
      const result = await this.getAlbumTracks(spotifyAlbumId, page, signal);
      if (signal?.aborted) throw signal.reason ?? new Error('Album track request aborted');
      const nextItems = result.items ?? [];
      items.push(...nextItems);
      totalCount = result.totalCount ?? items.length;
      if (nextItems.length === 0) break;
      page += 1;
    }

    return { items, totalCount: Number.isFinite(totalCount) ? totalCount : 0 };
  },
};
