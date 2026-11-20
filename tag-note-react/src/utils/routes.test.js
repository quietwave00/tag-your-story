import { describe, expect, it } from 'vitest';
import { buildRoute, routes } from './routes.js';

describe('catalog detail routes', () => {
  it('uses the search URL for catalog results', () => {
    expect(routes.tracks).toBe('/search');
  });

  it('keeps a unique ID and adds a readable slug for each subject', () => {
    expect(buildRoute.trackDetail('track-1', 'Song / Live')).toBe('/track/track-1/song-live');
    expect(buildRoute.albumDetail('album-1', 'A Night at the Opera')).toBe('/album/album-1/a-night-at-the-opera');
    expect(buildRoute.artistDetail('artist-1', 'Queen')).toBe('/artist/artist-1/queen');
    expect(buildRoute.trackDetail('track-2', 'Song / Live')).not.toBe(buildRoute.trackDetail('track-1', 'Song / Live'));
    expect(routes.trackDetailLegacy).toBe('/track/:trackId');
  });
});
