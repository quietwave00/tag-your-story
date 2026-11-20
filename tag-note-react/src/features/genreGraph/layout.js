import { forceSimulation, forceManyBody, forceLink, forceCollide, forceCenter } from 'd3-force-3d';

export function layoutGraph({ nodes, edges, components }) {
  const all = new Map(nodes.map(n => [n.id, n]));
  const boxes = [];
  for (const ids of components.filter(c => c.length > 1)) {
    const set = new Set(ids);
    const group = ids.map((id, i) => ({ ...all.get(id), x: Math.cos(i * 2.39996) * Math.sqrt(i) * 15, y: Math.sin(i * 2.39996) * Math.sqrt(i) * 15 }));
    const links = edges.filter(e => set.has(e.source) && set.has(e.target)).map(e => ({ ...e }));
    const sim = forceSimulation(group, 2).stop()
      .force('charge', forceManyBody().strength(-70))
      .force('link', forceLink(links).id(n => n.id).distance(e => e.type === 'FUSION' ? 28 : 55))
      .force('collide', forceCollide(n => n.radius + 5))
      .force('center', forceCenter(0, 0));
    sim.tick(220); sim.stop();
    const minX = Math.min(...group.map(n => n.x)), minY = Math.min(...group.map(n => n.y));
    const width = Math.max(...group.map(n => n.x)) - minX + 100;
    const height = Math.max(...group.map(n => n.y)) - minY + 100;
    boxes.push({ group: group.map(n => ({ id: n.id, x: n.x - minX + 50, y: n.y - minY + 50 })), width, height });
  }
  const result = [];
  const rowWidth = Math.max(800, boxes[0]?.width || 0);
  let x = 0, y = 0, rowHeight = 0;
  for (const box of boxes) {
    if (x && x + box.width > rowWidth) { y += rowHeight; x = 0; rowHeight = 0; }
    result.push(...box.group.map(n => ({ ...n, x: n.x + x, y: n.y + y })));
    x += box.width; rowHeight = Math.max(rowHeight, box.height);
  }
  y += rowHeight + 60;
  const isolated = components.filter(c => c.length === 1).flat();
  const cols = Math.max(1, Math.floor(rowWidth / 55));
  isolated.forEach((id, i) => result.push({ id, x: (i % cols) * 55, y: y + Math.floor(i / cols) * 55 }));
  return result;
}
