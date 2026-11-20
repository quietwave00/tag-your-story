import { layoutGraph } from './layout.js';

self.onmessage = ({ data }) => {
  try { self.postMessage({ positions: layoutGraph(data) }); }
  catch { self.postMessage({ error: true }); }
};
