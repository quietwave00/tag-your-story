import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useGenreGraph } from './useGenreGraph.js';
import { loadGenreGraph } from '../../services/genreGraphService.js';
vi.mock('../../services/genreGraphService.js', () => ({ loadGenreGraph: vi.fn() }));
afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.clearAllMocks(); });
const raw = { schemaVersion: 1, graphVersion: 'v1', completeness: 'PARTIAL', nodes: [{ id: 'a', name: 'A' }], edges: [] };
describe('genre loading lifecycle', () => {
  it('waits for layout, retains partial status and terminates the worker', async () => {
    const workers = [];
    vi.stubGlobal('Worker', class { constructor() { this.postMessage = vi.fn(); this.terminate = vi.fn(); workers.push(this); } });
    loadGenreGraph.mockResolvedValue(raw);
    const { result, unmount } = renderHook(useGenreGraph);
    await waitFor(() => expect(workers).toHaveLength(1));
    expect(result.current.status).toBe('loading');
    act(() => workers[0].onmessage({ data: { positions: [{ id: 'a', x: 1, y: 2 }] } }));
    expect(result.current.graph.completeness).toBe('PARTIAL');
    expect(result.current.status).toBe('ready');
    expect(workers[0].terminate).toHaveBeenCalled();
    unmount();
    expect(loadGenreGraph.mock.calls[0][0].aborted).toBe(true);
  });
  it('ignores responses arriving after unmount', async () => {
    let finish;
    loadGenreGraph.mockReturnValue(new Promise(resolve => { finish = resolve; }));
    const Worker = vi.fn(); vi.stubGlobal('Worker', Worker);
    const { unmount } = renderHook(useGenreGraph); unmount();
    await act(async () => finish(raw));
    expect(Worker).not.toHaveBeenCalled();
  });
  it('retries a failed load and represents an empty snapshot without starting a worker', async () => {
    loadGenreGraph.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ ...raw, nodes: [] });
    const { result } = renderHook(useGenreGraph);
    await waitFor(() => expect(result.current.status).toBe('error'));
    act(() => result.current.retry());
    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(result.current.graph.nodes).toEqual([]);
  });
});
