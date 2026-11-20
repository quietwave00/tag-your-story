import { expect, it } from 'vitest';
import { layoutGraph } from './layout.js';
import { normalizeGraph } from './model.js';
it('keeps every node, separates components, and produces stable positions without mutating the model', () => {
  const graph = normalizeGraph({ schemaVersion: 1, graphVersion: 'v1', completeness: 'COMPLETE', nodes: ['a', 'b', 'c', 'd', 'e'].map(id => ({ id, name: id })), edges: [
    { source: 'a', target: 'b', type: 'FUSION' }, { source: 'c', target: 'd', type: 'SUBGENRE' },
  ] });
  const positions = layoutGraph(graph);
  expect(positions).toEqual(layoutGraph(graph));
  expect(new Set(positions.map(n => n.id)).size).toBe(5);
  expect(positions.every(n => Number.isFinite(n.x) && Number.isFinite(n.y))).toBe(true);
  expect(graph.nodes.every(n => n.x === undefined)).toBe(true);
  expect(graph.edges[0].source).toBe('a');
  const byId = new Map(positions.map(n => [n.id, n]));
  expect(byId.get('e').y).toBeGreaterThan(Math.max(...positions.filter(n => n.id !== 'e').map(n => n.y)));
});
