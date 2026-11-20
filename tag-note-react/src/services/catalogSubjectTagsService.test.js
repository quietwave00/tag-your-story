import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient.js';
import { catalogSubjectTagsService } from './catalogSubjectTagsService.js';

afterEach(() => vi.restoreAllMocks());

describe('catalogSubjectTagsService', () => {
  it.each([
    ['TRACK', 'tracks'],
    ['ALBUM', 'albums'],
  ])('imports %s before reading its stored tags', async (subjectType, path) => {
    const calls = [];
    vi.spyOn(apiClient, 'post').mockImplementation(async (...args) => { calls.push(['post', ...args]); return {}; });
    vi.spyOn(apiClient, 'get').mockImplementation(async (...args) => { calls.push(['get', ...args]); return { tagDisplayStatus: 'EMPTY' }; });
    const signal = new AbortController().signal;

    await catalogSubjectTagsService.load(subjectType, 'spotify/id', signal);

    expect(calls).toEqual([
      ['post', '/api/catalog/import', { subjectType, spotifyId: 'spotify/id' }, { signal }],
      ['get', `/api/catalog/${path}/spotify%2Fid/tags`, { signal }],
    ]);
  });

  it('does not read tags when import fails', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValue(new Error('Import failed'));
    const get = vi.spyOn(apiClient, 'get');

    await expect(catalogSubjectTagsService.load('TRACK', 'track-1')).rejects.toThrow('Import failed');
    expect(get).not.toHaveBeenCalled();
  });

  it('does not read tags when navigation aborts during import', async () => {
    const controller = new AbortController();
    vi.spyOn(apiClient, 'post').mockImplementation(async () => { controller.abort(); return {}; });
    const get = vi.spyOn(apiClient, 'get');

    await expect(catalogSubjectTagsService.load('ALBUM', 'album-1', controller.signal)).rejects.toThrow();
    expect(get).not.toHaveBeenCalled();
  });
});
