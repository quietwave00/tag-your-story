import { describe, expect, it } from 'vitest';
import { normalizeGraph, neighborhood, searchNodes, safeSourceUrl, TYPES } from './model.js';

const node = (id, name = id) => ({ id, name, source: 'MUSICBRAINZ' });
const edge = (source, target, type = 'INFLUENCES') => ({ source, target, type, evidence: [] });
const snapshot = (edges = []) => ({ schemaVersion: 1, graphVersion: 'v1', completeness: 'COMPLETE', nodes: [node('a'), node('b'), node('c'), node('d')], edges });
describe('genre graph model', () => {
  it('preserves directions, multiple parents and cycles while counting unique neighbors', () => {
    const raw = snapshot([edge('a', 'b'), edge('b', 'a'), edge('a', 'b', 'SUBGENRE'), edge('c', 'b', 'SUBGENRE')]);
    const graph = normalizeGraph(raw);
    expect(graph.edges).toHaveLength(4);
    expect(graph.adjacency.get('a').size).toBe(1);
    expect(graph.incident.get('a')).toHaveLength(3);
    expect([...neighborhood(graph, 'b', ['SUBGENRE'])].sort()).toEqual(['a', 'b', 'c']);
    expect([...neighborhood(graph, 'b', [])]).toEqual(['b']);
    expect(graph.components.map(c => c.length)).toEqual([3, 1]);
    graph.nodes[0].x = 99;
    expect(raw.nodes[0].x).toBeUndefined();
  });
  it('keeps isolated nodes and self references without counting self as a neighbor', () => {
    const graph = normalizeGraph(snapshot([edge('a', 'a')]));
    expect(graph.adjacency.get('a').size).toBe(0);
    expect(graph.incident.get('a')).toHaveLength(1);
    expect(neighborhood(graph, 'd', TYPES)).toEqual(new Set(['d']));
  });
  it.each([
    raw => { raw.schemaVersion = 2; },
    raw => { raw.nodes.push(node('a')); },
    raw => { raw.edges.push(edge('missing', 'a')); },
    raw => { raw.edges.push(edge('a', 'b', 'UNKNOWN')); },
    raw => { raw.edges.push(edge('a', 'b'), edge('a', 'b')); },
  ])('rejects invalid data rather than silently changing relationships', change => {
    const raw = snapshot(); change(raw); expect(() => normalizeGraph(raw)).toThrow();
  });
  it('accepts empty and partial snapshots explicitly', () => {
    expect(normalizeGraph({ ...snapshot(), nodes: [] }).nodes).toEqual([]);
    expect(normalizeGraph({ ...snapshot(), completeness: 'PARTIAL' }).completeness).toBe('PARTIAL');
  });
  it('sorts exact, prefix and substring matches without rewriting names', () => {
    const nodes = [node('3', 'Jazz rock'), node('2', 'Rock'), node('1', 'Rockabilly')];
    expect(searchNodes(nodes, ' ROCK ').map(n => n.name)).toEqual(['Rock', 'Rockabilly', 'Jazz rock']);
  });
  it('does not expose executable source URLs', () => {
    expect(safeSourceUrl('javascript:alert(1)')).toBeNull();
    expect(safeSourceUrl('https://musicbrainz.org/genre/a')).toBe('https://musicbrainz.org/genre/a');
  });
});
