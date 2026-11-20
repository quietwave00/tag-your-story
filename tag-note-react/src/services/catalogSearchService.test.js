import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient.js';
import { catalogSearchService } from './catalogSearchService.js';

afterEach(() => vi.restoreAllMocks());

const emptyResponse = {
  tracks: { items: [], totalCount: 0, hasNext: false },
  albums: { items: [], totalCount: 0, hasNext: false },
  artists: { items: [], totalCount: 0, hasNext: false },
};

describe('catalogSearchService', () => {
  it('requests all sections once and converts independent UI pages to zero-based API pages', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue(emptyResponse);
    const signal = new AbortController().signal;

    await catalogSearchService.searchScreen({ keyword: 'Queen', trackPage: 2, albumPage: 4, artistPage: 3, signal });

    expect(get).toHaveBeenCalledTimes(1);
    expect(get).toHaveBeenCalledWith('/api/catalog/search/screen', {
      params: { keyword: 'Queen', trackPage: 1, albumPage: 3, artistPage: 2 }, signal,
    });
  });

  it('rejects a section with the wrong item type or missing hasNext', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      ...emptyResponse,
      tracks: { items: [{ subjectType: 'ALBUM' }], totalCount: 1, hasNext: true },
    }).mockResolvedValueOnce({
      ...emptyResponse,
      artists: { items: [], totalCount: 1 },
    });

    await expect(catalogSearchService.searchScreen({ keyword: 'Queen', trackPage: 1, albumPage: 1, artistPage: 1 }))
      .rejects.toThrow('TRACK contract');
    await expect(catalogSearchService.searchScreen({ keyword: 'Queen', trackPage: 1, albumPage: 1, artistPage: 1 }))
      .rejects.toThrow('ARTIST contract');
  });
});
