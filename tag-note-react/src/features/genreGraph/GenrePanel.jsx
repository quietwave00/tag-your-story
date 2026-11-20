import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { neighborhood, safeSourceUrl, TYPES } from './model.js';

export default function GenrePanel({ graph, selected, types, onSelect, onClose }) {
  const { t } = useTranslation('genres');
  const heading = useRef(null);
  const [expanded, setExpanded] = useState(true);
  const node = graph.byId.get(selected);
  const relations = graph.incident.get(selected);
  const visible = relations.filter(e => types.includes(e.type));
  useEffect(() => { heading.current?.focus({ preventScroll: true }); }, [selected]);
  const url = safeSourceUrl(node.url);
  return <aside className={`genre-panel${expanded ? '' : ' genre-panel--collapsed'}`} aria-labelledby="genre-panel-title" onKeyDown={e => { if (e.key === 'Escape') onClose(); }}>
    <div className="genre-panel-heading"><h2 id="genre-panel-title" tabIndex={-1} ref={heading}>{node.name}</h2>
      <button className="genre-panel-toggle" type="button" aria-expanded={expanded} onClick={() => setExpanded(v => !v)}>{t(expanded ? 'collapse' : 'expand')}</button>
      <button type="button" onClick={onClose} aria-label={t('closePanel')}>×</button>
    </div>
    <div className="genre-panel-body">
      <p>
        {node.source}
        {url && <> · <a href={url} target="_blank" rel="noopener noreferrer">{t('source')}</a></>}
      </p>
      <p>{t('neighbors', { visible: Math.max(0, neighborhood(graph, selected, types).size - 1), total: graph.adjacency.get(selected).size })}</p>
      <p>{t('relations', { visible: visible.length, total: relations.length })}</p>
      {!relations.length && <p>{t('isolated')}</p>}
      {!!relations.length && !visible.length && <p>{t('filteredEmpty')}</p>}
      {TYPES.flatMap(type => ['in', 'out'].map(direction => {
        const list = visible.filter(e => e.type === type && (direction === 'in' ? e.target : e.source) === selected);
        if (!list.length) return null;
        return <section key={`${type}-${direction}`}><h3>{t(`groups.${type}.${direction}`)}</h3><ul>
          {list.map(e => {
            const target = graph.byId.get(direction === 'in' ? e.source : e.target);
            return <li key={e.key}><button type="button" className="genre-related" onClick={() => onSelect(target.id)}>{target.name}</button>
              {!!e.evidence.length && <details><summary>{t('evidence')}</summary><ul>{e.evidence.map((item, i) => {
                const href = safeSourceUrl(item.pageUrl);
                return <li key={i}>
                  {item.rawLabel}
                  {href && <> · <a href={href} target="_blank" rel="noopener noreferrer">{t('source')}</a></>}
                </li>;
              })}</ul></details>}
            </li>;
          })}
        </ul></section>;
      }))}
    </div>
  </aside>;
}
