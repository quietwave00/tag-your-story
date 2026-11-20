import { useEffect, useState } from 'react';
import { loadGenreGraph } from '../../services/genreGraphService.js';
import { normalizeGraph } from './model.js';

export function useGenreGraph() {
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState({ status: 'loading' });
  useEffect(() => {
    const controller = new AbortController();
    let worker;
    const started = performance.now();
    loadGenreGraph(controller.signal).then(raw => {
      if (controller.signal.aborted) return;
      const graph = normalizeGraph(raw);
      if (!graph.nodes.length) { setState({ status: 'ready', graph, positions: [], elapsed: performance.now() - started }); return; }
      worker = new Worker(new URL('./layout.worker.js', import.meta.url), { type: 'module' });
      worker.onmessage = ({ data }) => {
        if (controller.signal.aborted) return;
        setState(data.error ? { status: 'error' } : { status: 'ready', graph, positions: data.positions, elapsed: performance.now() - started });
        worker.terminate();
      };
      worker.onerror = () => { if (!controller.signal.aborted) setState({ status: 'error' }); worker.terminate(); };
      worker.postMessage({ nodes: graph.nodes, edges: graph.edges, components: graph.components });
    }).catch(() => { if (!controller.signal.aborted) setState({ status: 'error' }); });
    return () => { controller.abort(); worker?.terminate(); };
  }, [attempt]);
  return { ...state, retry: () => { setState({ status: 'loading' }); setAttempt(a => a + 1); } };
}
