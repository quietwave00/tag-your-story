import { afterEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './apiClient.js';
import { catalogDetailService } from './catalogDetailService.js';

afterEach(() => vi.restoreAllMocks());

describe('catalogDetailService', () => {
  it('uses encoded Spotify IDs and converts UI pages to zero-based API pages', async () => {
    const get = vi.spyOn(apiClient, 'get').mockResolvedValue({ items: [], totalCount: 0 });
    const signal = new AbortController().signal;

    await catalogDetailService.getArtist('artist/id', signal);
    await catalogDetailService.getArtistAlbums('artist/id', 2, 'ALBUM', signal);
    await catalogDetailService.getAlbum('album/id', signal);
    await catalogDetailService.getAlbumTracks('album/id', 3, signal);

    expect(get).toHaveBeenNthCalledWith(1, '/api/catalog/artists/artist%2Fid', { signal });
    expect(get).toHaveBeenNthCalledWith(2, '/api/catalog/artists/artist%2Fid/albums', { params: { page: 1, releaseType: 'ALBUM' }, signal });
    expect(get).toHaveBeenNthCalledWith(3, '/api/catalog/albums/album%2Fid', { signal });
    expect(get).toHaveBeenNthCalledWith(4, '/api/catalog/albums/album%2Fid/tracks', { params: { page: 2 }, signal });
  });

  it('loads every Album track page in order until the reported total is reached', async () => {
    const get = vi.spyOn(apiClient, 'get').mockImplementation((path, { params }) => Promise.resolve({
      items: params.page === 0 ? Array.from({ length: 10 }, (_, index) => ({ spotifyId: `track-${index + 1}` })) : [{ spotifyId: 'track-11' }],
      totalCount: 11,
    }));

    const result = await catalogDetailService.getAllAlbumTracks('album/id');

    expect(result.items.map((item) => item.spotifyId)).toEqual(Array.from({ length: 11 }, (_, index) => `track-${index + 1}`));
    expect(get).toHaveBeenCalledTimes(2);
    expect(get).toHaveBeenNthCalledWith(2, '/api/catalog/albums/album%2Fid/tracks', { params: { page: 1 }, signal: undefined });
  });
});
