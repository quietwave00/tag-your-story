export const TYPES = ['SUBGENRE', 'INFLUENCES', 'FUSION'];
const compare = (a, b) => a < b ? -1 : a > b ? 1 : 0;
export function normalizeGraph(raw) {
  if (raw?.schemaVersion !== 1 || !Array.isArray(raw.nodes) || !Array.isArray(raw.edges)
    || typeof raw.graphVersion !== 'string' || !['COMPLETE', 'PARTIAL'].includes(raw.completeness)) throw new Error('Invalid graph');
  const nodes = raw.nodes.map((n) => {
    if (typeof n.id !== 'string' || !n.id || typeof n.name !== 'string') throw new Error('Invalid node');
    return { ...n };
  }).sort((a, b) => compare(a.id, b.id));
  const byId = new Map(nodes.map(n => [n.id, n]));
  if (byId.size !== nodes.length) throw new Error('Duplicate node');
  const adjacency = new Map(nodes.map(n => [n.id, new Set()]));
  const incident = new Map(nodes.map(n => [n.id, []]));
  const keys = new Set();
  const edges = raw.edges.map(e => {
    const key = JSON.stringify([e.source, e.target, e.type]);
    if (!byId.has(e.source) || !byId.has(e.target) || !TYPES.includes(e.type) || keys.has(key)) throw new Error('Invalid edge');
    keys.add(key);
    if (e.source !== e.target) {
      adjacency.get(e.source).add(e.target); adjacency.get(e.target).add(e.source);
    }
    const edge = { ...e, key, evidence: Array.isArray(e.evidence) ? e.evidence : [] };
    incident.get(e.source).push(edge);
    if (e.source !== e.target) incident.get(e.target).push(edge);
    return edge;
  });
  const unseen = new Set(byId.keys());
  const components = [];
  for (const node of nodes) {
    if (!unseen.delete(node.id)) continue;
    const ids = [node.id];
    for (let i = 0; i < ids.length; i++) for (const id of adjacency.get(ids[i])) if (unseen.delete(id)) ids.push(id);
    components.push(ids.sort(compare));
  }
  components.sort((a, b) => b.length - a.length || compare(a[0], b[0]));
  const maxDegree = Math.max(1, ...nodes.map(n => adjacency.get(n.id).size));
  nodes.forEach(n => { n.degree = adjacency.get(n.id).size; n.radius = 3 + 5 * Math.log1p(n.degree) / Math.log1p(maxDegree); });
  return { nodes, edges, byId, adjacency, incident, components, graphVersion: raw.graphVersion, completeness: raw.completeness, collectedAt: raw.collectedAt, scope: raw.scope };
}
export function neighborhood(graph, id, types) {
  const ids = new Set(id ? [id] : []);
  for (const e of graph.incident.get(id) || []) if (types.includes(e.type)) { ids.add(e.source); ids.add(e.target); }
  return ids;
}
export function searchNodes(nodes, query) {
  const q = query.trim().toLowerCase();
  const rank = n => n.name.toLowerCase() === q ? 0 : n.name.toLowerCase().startsWith(q) ? 1 : 2;
  return nodes.filter(n => n.name.toLowerCase().includes(q)).sort((a, b) => rank(a) - rank(b) || compare(a.name.toLowerCase(), b.name.toLowerCase()) || compare(a.id, b.id));
}
export function safeSourceUrl(value) {
  try { const url = new URL(value); return ['https:', 'http:'].includes(url.protocol) ? url.href : null; } catch { return null; }
}
