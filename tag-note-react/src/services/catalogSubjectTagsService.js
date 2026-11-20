import { apiClient } from './apiClient.js';

const TAG_PATHS = {
  TRACK: 'tracks',
  ALBUM: 'albums',
};

export const catalogSubjectTagsService = {
  async load(subjectType, spotifyId, signal) {
    const path = TAG_PATHS[subjectType];
    if (!path) throw new Error(`Unsupported catalog subject type: ${subjectType}`);

    await apiClient.post('/api/catalog/import', { subjectType, spotifyId }, { signal });
    if (signal?.aborted) throw signal.reason ?? new Error('Catalog tag request aborted');
    return apiClient.get(`/api/catalog/${path}/${encodeURIComponent(spotifyId)}/tags`, { signal });
  },
};
