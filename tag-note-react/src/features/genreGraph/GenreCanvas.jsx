import { useEffect, useMemo, useRef, useState } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import { useTranslation } from 'react-i18next';
import { neighborhood, TYPES } from './model.js';

const colors = { SUBGENRE: '#516b60', INFLUENCES: '#85634d', FUSION: '#666083' };
export default function GenreCanvas({ graph, positions, selected, types, onSelect, resetKey }) {
  const { t } = useTranslation('genres');
  const container = useRef(null), api = useRef(null);
  const [size, setSize] = useState({ width: 600, height: 500 });
  const [hover, setHover] = useState(null);
  const labelRects = useRef([]), pointerStart = useRef(null);
  const highlighted = useMemo(() => neighborhood(graph, selected, types), [graph, selected, types]);
  const fitIds = useMemo(() => neighborhood(graph, selected, TYPES), [graph, selected]);
  const data = useMemo(() => {
    const points = new Map(positions.map(p => [p.id, p]));
    const nodes = graph.nodes.map(n => ({ ...n, ...points.get(n.id), fx: points.get(n.id)?.x, fy: points.get(n.id)?.y }));
    const pairs = new Map();
    graph.edges.forEach(e => { const key = JSON.stringify([e.source, e.target].sort()); pairs.set(key, [...(pairs.get(key) || []), e.key]); });
    const links = graph.edges.map(e => {
      const group = pairs.get(JSON.stringify([e.source, e.target].sort()));
      return { ...e, bend: (group.indexOf(e.key) - (group.length - 1) / 2) * 20 * (e.source < e.target ? 1 : -1) };
    });
    return { nodes, links };
  }, [graph, positions]);
  const labelNodes = useMemo(() => [...data.nodes].sort((a, b) => {
    const rank = n => n.id === selected ? 0 : n.id === hover ? 1 : highlighted.has(n.id) ? 2 : 3;
    return rank(a) - rank(b) || b.degree - a.degree || a.id.localeCompare(b.id);
  }), [data, selected, hover, highlighted]);
  useEffect(() => {
    const observer = new ResizeObserver(([entry]) => setSize({ width: entry.contentRect.width, height: entry.contentRect.height }));
    observer.observe(container.current);
    return () => observer.disconnect();
  }, []);
  useEffect(() => {
    let frame;
    let cancelled = false;
    frame = requestAnimationFrame(() => {
      const camera = api.current;
      if (!camera || cancelled) return;
      const focus = data.nodes.find(n => n.id === selected);
      const targets = focus ? data.nodes.filter(n => fitIds.has(n.id)) : data.nodes;
      if (!targets.length) return;
      const minX = Math.min(...targets.map(n => n.x - n.radius));
      const maxX = Math.max(...targets.map(n => n.x + n.radius));
      const minY = Math.min(...targets.map(n => n.y - n.radius));
      const maxY = Math.max(...targets.map(n => n.y + n.radius));
      const x = focus ? focus.x : (minX + maxX) / 2;
      const y = focus ? focus.y : (minY + maxY) / 2;
      const width = 2 * Math.max(x - minX, maxX - x, 30);
      const height = 2 * Math.max(y - minY, maxY - y, 30);
      const zoom = Math.max(0.03, Math.min(focus ? 3 : 8, (size.width - 100) / width, (size.height - 100) / height));
      const start = camera.centerAt();
      const startZoom = camera.zoom();
      const duration = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 750;
      const started = performance.now();
      function move(now) {
        if (cancelled) return;
        const progress = duration ? Math.min(1, (now - started) / duration) : 1;
        const eased = progress * progress * (3 - 2 * progress);
        camera.centerAt(start.x + (x - start.x) * eased, start.y + (y - start.y) * eased, 0);
        camera.zoom(startZoom + (zoom - startZoom) * eased, 0);
        if (progress < 1) frame = requestAnimationFrame(move);
      }
      frame = requestAnimationFrame(move);
    });
    const element = container.current;
    const stop = () => { cancelled = true; cancelAnimationFrame(frame); };
    element.addEventListener('pointerdown', stop);
    element.addEventListener('wheel', stop, { passive: true });
    return () => { stop(); element.removeEventListener('pointerdown', stop); element.removeEventListener('wheel', stop); };
  }, [selected, fitIds, size, resetKey, data]);
  function drawNode(n, ctx, scale) {
    ctx.beginPath(); ctx.arc(n.x, n.y, n.radius, 0, Math.PI * 2);
    ctx.fillStyle = selected && !highlighted.has(n.id) ? '#d6d6d6' : '#292929'; ctx.fill();
    if (n.id === selected || n.id === hover) {
      ctx.beginPath(); ctx.arc(n.x, n.y, n.radius + 3 / scale, 0, Math.PI * 2);
      ctx.strokeStyle = '#222'; ctx.lineWidth = 1 / scale; ctx.stroke();
    }
  }
  function drawLink(e, ctx, scale) {
    if (!types.includes(e.type)) return;
    const a = e.source, b = e.target;
    if (typeof a !== 'object' || typeof b !== 'object') return;
    const active = selected && (a.id === selected || b.id === selected);
    const dx = b.x - a.x, dy = b.y - a.y, length = Math.hypot(dx, dy) || 1;
    const nx = -dy / length, ny = dx / length;
    ctx.strokeStyle = active ? colors[e.type] : selected ? '#e2e2e2' : '#ccc';
    ctx.lineWidth = (active ? 1.5 : 0.6) / scale;
    ctx.beginPath();
    if (a.id === b.id) { ctx.arc(a.x + 12, a.y - 12, 15, 0, Math.PI * 1.8); ctx.stroke(); return; }
    const point = u => {
      const wave = e.type === 'INFLUENCES' ? Math.sin(u * Math.PI * 12) * Math.sin(u * Math.PI) * 2 / scale : 0;
      const offset = Math.sin(u * Math.PI) * e.bend + wave;
      return [a.x + dx * u + nx * offset, a.y + dy * u + ny * offset];
    };
    const steps = e.type === 'INFLUENCES' && active ? 48 : 16;
    for (let i = 0; i <= steps; i++) { const [x, y] = point(i / steps); if (!i) ctx.moveTo(x, y); else ctx.lineTo(x, y); }
    ctx.stroke();
    if (active || scale > 1.3) {
      const u = Math.max(0.5, 1 - (b.radius + 4 / scale) / length);
      const [x, y] = point(u), [px, py] = point(Math.max(0, u - 0.02));
      const angle = Math.atan2(y - py, x - px), arrow = (active ? 13 : 10) / scale;
      ctx.beginPath(); ctx.moveTo(x, y);
      ctx.lineTo(x - Math.cos(angle - 0.5) * arrow, y - Math.sin(angle - 0.5) * arrow);
      ctx.lineTo(x - Math.cos(angle + 0.5) * arrow, y - Math.sin(angle + 0.5) * arrow);
      ctx.closePath(); ctx.fillStyle = ctx.strokeStyle; ctx.fill();
    }
  }
  function drawLabels(ctx, scale) {
    const cells = new Map();
    labelRects.current = [];
    for (const n of labelNodes) {
      const priority = n.id === selected || n.id === hover;
      if (!priority && selected && !highlighted.has(n.id) && scale < 1) continue;
      if (!priority && !highlighted.has(n.id) && scale < 0.4 && n.degree < 8) continue;
      const pos = api.current?.graph2ScreenCoords(n.x, n.y);
      if (!pos || pos.x < 0 || pos.y < 0 || pos.x > size.width || pos.y > size.height) continue;
      const fontSize = priority ? 14 : 11;
      ctx.font = `${priority ? 700 : 600} ${fontSize / scale}px sans-serif`;
      const width = ctx.measureText(n.name).width * scale;
      const rect = { x: pos.x + n.radius * scale + 4, y: pos.y - fontSize / 2, w: width + 6, h: fontSize + 4 };
      const keys = [];
      for (let x = Math.floor(rect.x / 64); x <= Math.floor((rect.x + rect.w) / 64); x++)
        for (let y = Math.floor(rect.y / 32); y <= Math.floor((rect.y + rect.h) / 32); y++) keys.push(`${x}:${y}`);
      const overlaps = keys.some(key => (cells.get(key) || []).some(r => rect.x < r.x + r.w && rect.x + rect.w > r.x && rect.y < r.y + r.h && rect.y + rect.h > r.y));
      if (!priority && overlaps) continue;
      labelRects.current.push({ ...rect, id: n.id });
      keys.forEach(key => cells.set(key, [...(cells.get(key) || []), rect]));
      const x = n.x + n.radius + 4 / scale;
      ctx.fillStyle = 'rgba(242,242,242,0.88)'; ctx.fillRect(x - 2 / scale, n.y - (fontSize / 2 + 2) / scale, (width + 4) / scale, (fontSize + 4) / scale);
      ctx.fillStyle = selected && !highlighted.has(n.id) ? '#999' : '#171717'; ctx.textBaseline = 'middle'; ctx.fillText(n.name, x, n.y);
    }
  }
  return <div className="genre-canvas" ref={container} role="group" aria-label={t('canvas')}
    onPointerDownCapture={e => { pointerStart.current = { x: e.clientX, y: e.clientY }; }}
    onClickCapture={e => {
      if (e.target.tagName !== 'CANVAS') return;
      const start = pointerStart.current;
      if (!start || Math.hypot(e.clientX - start.x, e.clientY - start.y) > 5) return;
      const bounds = container.current.getBoundingClientRect();
      const x = e.clientX - bounds.left, y = e.clientY - bounds.top;
      const hit = labelRects.current.find(r => x >= r.x && x <= r.x + r.w && y >= r.y && y <= r.y + r.h);
      if (hit) { e.stopPropagation(); onSelect(hit.id); }
    }}>
    <div aria-hidden="true"><ForceGraph2D ref={api} width={size.width} height={size.height} graphData={data}
      backgroundColor="#f2f2f2" cooldownTicks={0} enableNodeDrag={false} nodeLabel={() => ''}
      nodeCanvasObject={drawNode} nodePointerAreaPaint={(n, color, ctx) => { ctx.fillStyle = color; ctx.beginPath(); ctx.arc(n.x, n.y, n.radius + 5, 0, 2 * Math.PI); ctx.fill(); }}
      linkCanvasObject={drawLink} linkCanvasObjectMode={() => 'replace'} onRenderFramePost={drawLabels}
      onNodeClick={n => onSelect(n.id)} onNodeHover={n => setHover(n?.id || null)} minZoom={0.03} maxZoom={8} /></div>
    <div className="genre-zoom"><button type="button" aria-label={t('zoomIn')} onClick={() => api.current?.zoom(api.current.zoom() * 1.4, 0)}>+</button><button type="button" aria-label={t('zoomOut')} onClick={() => api.current?.zoom(api.current.zoom() / 1.4, 0)}>−</button></div>
  </div>;
}
