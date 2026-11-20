import { lazy, Suspense, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useGenreGraph } from '../features/genreGraph/useGenreGraph.js';
import GenreSearch from '../features/genreGraph/GenreSearch.jsx';
import { TYPES } from '../features/genreGraph/model.js';
import '../styles/genres.css';
const GenreCanvas = lazy(() => import('../features/genreGraph/GenreCanvas.jsx'));

export default function GenresPage() {
  const { t } = useTranslation('genres');
  const data = useGenreGraph();
  const [params, setParams] = useSearchParams();
  const selectedId = params.get('node');
  const [types, setTypes] = useState(TYPES), [resetKey, setResetKey] = useState(0);
  const [notice, setNotice] = useState(false);
  const searchInput = useRef(null);
  const selected = data.graph?.byId.has(selectedId) ? selectedId : null;
  const invalid = data.status === 'ready' && selectedId !== null && !selected;
  useEffect(() => {
    if (!invalid) return;
    setNotice(true);
    setParams(previous => { const next = new URLSearchParams(previous); next.delete('node'); return next; }, { replace: true });
  }, [invalid, setParams]);
  function choose(id) {
    setNotice(false);
    if (id === selected) { setResetKey(k => k + 1); return; }
    setParams(previous => { const next = new URLSearchParams(previous); next.set('node', id); return next; });
  }
  function close() {
    setParams(previous => { const next = new URLSearchParams(previous); next.delete('node'); return next; });
    searchInput.current?.focus();
  }
  function reset() { close(); setTypes(TYPES); setResetKey(k => k + 1); setNotice(false); }
  const counts = useMemo(() => Object.fromEntries(TYPES.map(type => [type, data.graph?.edges.filter(e => e.type === type).length || 0])), [data.graph]);
  return <section className="genres-page" aria-labelledby="genres-title">
    <header className="genre-title"><h1 id="genres-title">{t('title')}</h1>{data.graph && <p>{t('totals', { nodes: data.graph.nodes.length, edges: data.graph.edges.length })}</p>}</header>
    {data.status === 'loading' && <div className="genre-loading skeleton-block" role="status">{t('loading')}</div>}
    {data.status === 'error' && <div role="alert"><p>{t('error')}</p><button type="button" onClick={data.retry}>{t('retry')}</button></div>}
    {data.status === 'ready' && <>
      {data.graph.completeness === 'PARTIAL' && <p role="status">{t('partial')}</p>}
      {(invalid || (notice && !selected)) && <p role="status">{t('invalidNode')}</p>}
      {!data.graph.nodes.length ? <p>{t('empty')}</p> : <>
        <div className="genre-toolbar">
          <GenreSearch nodes={data.graph.nodes} onSelect={choose} inputRef={searchInput} />
          <fieldset className="genre-filters"><legend className="visually-hidden">{t('filter')}</legend>{TYPES.map(type => <label key={type}>
            <input type="checkbox" checked={types.includes(type)} onChange={() => setTypes(current => current.includes(type) ? current.filter(v => v !== type) : [...current, type])} />{t(`types.${type}`)} <span>{counts[type]}</span>
          </label>)}</fieldset>
          <button type="button" onClick={reset}>{t('overview')}</button>
        </div>
        <p className="visually-hidden" role="status">{selected ? data.graph.byId.get(selected).name : ""}</p>
        <div className="genre-workspace">
          <Suspense fallback={<div className="genre-loading" role="status">{t('loading')}</div>}><GenreCanvas graph={data.graph} positions={data.positions} selected={selected} types={types} onSelect={choose} resetKey={resetKey} /></Suspense>
        </div>
        <div className="genre-legend" aria-label={t('legend')}>{TYPES.map(type => <span key={type} className={`genre-key genre-key--${type.toLowerCase()}`}>{t(`legendText.${type}`)}</span>)}</div>
        <p className="genre-help">{t('help')}</p>
      </>}
      <details className="genre-data"><summary>{t('dataInfo')}</summary>
        <p>{t('interpretation')}</p><p>{t('completeMeaning')}</p>
        <dl><dt>{t('version')}</dt><dd>{data.graph.graphVersion}</dd><dt>{t('collected')}</dt><dd>{data.graph.collectedAt}</dd><dt>{t('scope')}</dt><dd>{data.graph.scope} / {data.graph.completeness}</dd></dl>
        <p>{t('components', { count: data.graph.components.length, isolated: data.graph.nodes.filter(n => n.degree === 0).length })}</p>
      </details>
    </>}
  </section>;
}
