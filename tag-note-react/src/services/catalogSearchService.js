import { apiClient } from './apiClient.js';

export const catalogSearchService = {
  async searchScreen({ keyword, trackPage, albumPage, artistPage, signal }) {
    const response = await apiClient.get('/api/catalog/search/screen', {
      params: { keyword, trackPage: trackPage - 1, albumPage: albumPage - 1, artistPage: artistPage - 1 },
      signal,
    });
    for (const [section, type] of [['tracks', 'TRACK'], ['albums', 'ALBUM'], ['artists', 'ARTIST']]) {
      const result = response?.[section];
      if (!Array.isArray(result?.items)
        || result.items.some((item) => item.subjectType !== type)
        || typeof result.hasNext !== 'boolean') {
        throw new Error(`Catalog screen response does not match the ${type} contract`);
      }
    }
    return response;
  },
};
