import { describe, expect, it } from 'vitest';
import { createDetailNavigationState, getDetailBackTarget } from './detailNavigation.js';

describe('detail navigation', () => {
  it('restores search context after multiple detail transitions', () => {
    const artist = { pathname: '/artist/artist-1/name', search: '', state: { fromSearch: '/search?keyword=Jazz&page=2' } };
    const album = { pathname: '/album/album-1/title', search: '', state: createDetailNavigationState(artist) };
    const track = { pathname: '/track/track-1/song', search: '', state: createDetailNavigationState(album) };

    const backToAlbum = getDetailBackTarget(track);
    expect(backToAlbum.path).toBe('/album/album-1/title');
    const backToArtist = getDetailBackTarget({ ...album, state: backToAlbum.state });
    expect(backToArtist.path).toBe('/artist/artist-1/name');
    expect(getDetailBackTarget({ ...artist, state: backToArtist.state })).toEqual({
      path: '/search?keyword=Jazz&page=2', state: null,
    });
  });
});
