import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { searchNodes } from './model.js';

export default function GenreSearch({ nodes, onSelect, inputRef }) {
  const { t } = useTranslation('genres');
  const [query, setQuery] = useState(''), [open, setOpen] = useState(false);
  const [limit, setLimit] = useState(20), [active, setActive] = useState(-1);
  const results = useMemo(() => searchNodes(nodes, query), [nodes, query]);
  const shown = results.slice(0, limit);
  useEffect(() => {
    if (open && active >= 0) document.getElementById(`genre-result-${active}`)?.scrollIntoView?.({ block: 'nearest' });
  }, [active, open]);
  function choose(id) { onSelect(id); setOpen(false); setActive(-1); }
  return <div className="genre-search" onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget)) setOpen(false); }}>
    <label className="visually-hidden" htmlFor="genre-search-input">{t('search')}</label>
    <input id="genre-search-input" ref={inputRef} role="combobox" autoComplete="off" aria-expanded={open}
      aria-controls="genre-search-results" aria-autocomplete="list" aria-activedescendant={open && shown[active] ? `genre-result-${active}` : undefined}
      placeholder={t('search')} value={query} onFocus={() => setOpen(true)} onChange={e => { setQuery(e.target.value); setOpen(true); setLimit(20); setActive(-1); }}
      onKeyDown={e => {
        if (e.key === 'Escape') { setOpen(false); setActive(-1); }
        if (e.key === 'ArrowDown' || e.key === 'ArrowUp') { e.preventDefault(); setOpen(true); setActive(i => Math.max(-1, Math.min(shown.length - 1, i + (e.key === 'ArrowDown' ? 1 : -1)))); }
        if (e.key === 'Enter' && open && shown[active]) { e.preventDefault(); choose(shown[active].id); }
      }} />
    {open && <div className="genre-search-popup">
      <div id="genre-search-results" role="listbox" aria-label={t('results')}>
        {shown.map((n, i) => <button key={n.id} id={`genre-result-${i}`} role="option" aria-selected={active === i} tabIndex={-1} type="button" onClick={() => choose(n.id)}>{n.name}</button>)}
      </div>
      {!results.length && <p role="status">{t('noResults')}</p>}
      {limit < results.length && <button type="button" onClick={() => setLimit(n => n + 20)}>{t('more')}</button>}
      <button type="button" onClick={() => { inputRef.current?.focus(); setOpen(false); }}>{t('closeSearch')}</button>
    </div>}
  </div>;
}
