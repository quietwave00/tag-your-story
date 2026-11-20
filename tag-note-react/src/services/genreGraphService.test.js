import { afterEach, expect, it, vi } from 'vitest';
import { loadGenreGraph } from './genreGraphService.js';
afterEach(() => vi.unstubAllGlobals());
it('loads the static asset with the caller cancellation signal', async () => {
  const fetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ graphVersion: 'v1' }) });
  vi.stubGlobal('fetch', fetch);
  const controller = new AbortController();
  expect(await loadGenreGraph(controller.signal)).toEqual({ graphVersion: 'v1' });
  expect(fetch).toHaveBeenCalledWith(expect.stringMatching(/data\/genre-graph\.json$/), { signal: controller.signal });
});
it('rejects failed HTTP responses instead of rendering an empty map', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false }));
  await expect(loadGenreGraph()).rejects.toThrow();
});
