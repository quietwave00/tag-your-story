import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { catalogSearchService } from '../services/catalogSearchService.js';
import { buildRoute, routes } from '../utils/routes.js';
import { saveSelectedTrack, saveTrackSearch } from '../utils/trackSearchStorage.js';
import '../styles/tracks.css';

const DEFAULT_PAGE = 1;
const SEARCH_TYPES = ['ARTIST', 'TRACK', 'ALBUM'];
const TYPE_LABEL_KEYS = { ARTIST: 'search.sections.ARTIST', TRACK: 'search.sections.TRACK', ALBUM: 'search.sections.ALBUM' };
const PAGE_PARAM_KEYS = { ARTIST: 'artistPage', TRACK: 'page', ALBUM: 'albumPage' };
const RESPONSE_KEYS = { ARTIST: 'artists', TRACK: 'tracks', ALBUM: 'albums' };
const LOADING_ARTISTS = [0, 1, 2, 3, 4];
const EMPTY_SECTION = { items: [], totalCount: 0, hasNext: false, error: null };

function getArtistVisibleCount() {
  if (window.innerWidth <= 520) return 2;
  if (window.innerWidth <= 767) return 4;
  return 5;
}

function getValidPage(pageParam) {
  const page = Number(pageParam);
  return Number.isInteger(page) && page > 0 ? page : DEFAULT_PAGE;
}

function getSectionKey(keyword, type, page) {
  return JSON.stringify([keyword, type, page]);
}

function CatalogImage({ item, t, artist = false }) {
  const className = artist ? 'catalog-artist-image' : 'track-image';
  if (!item.imageUrl || item.imageUrl === 'NO_IMAGE') {
    return <span aria-label={t('search.noImage')} className={`${className} track-image-empty`} role="img" />;
  }
  return <img alt={t('search.imageAlt', { title: item.title })} className={className} src={item.imageUrl} />;
}

function CatalogResultsSection({ keyword, type, currentPage, section, sectionKey, isLoading, onPageChange, fromSearch }) {
  const { t } = useTranslation(['tracks', 'common']);
  const navigate = useNavigate();
  const label = t(TYPE_LABEL_KEYS[type]);
  const { items, totalCount, hasNext, error } = section;
  const artistEntryRef = useRef(null);
  const [artistGroup, setArtistGroup] = useState({ requestKey: null, index: 0, side: 'index', direction: 'initial' });
  const [artistVisibleCount, setArtistVisibleCount] = useState(getArtistVisibleCount);
  const artistGroupCount = Math.max(1, Math.ceil(items.length / artistVisibleCount));
  const artistGroupIndex = artistGroup.requestKey === sectionKey
    ? artistGroup.side === 'end' ? artistGroupCount - 1 : Math.min(artistGroup.index, artistGroupCount - 1)
    : 0;
  const artistDirection = artistGroup.requestKey === sectionKey ? artistGroup.direction : 'initial';
  const visibleArtists = type === 'ARTIST'
    ? items.slice(artistGroupIndex * artistVisibleCount, (artistGroupIndex + 1) * artistVisibleCount)
    : [];

  useEffect(() => {
    if (type !== 'ARTIST') return undefined;
    const onResize = () => setArtistVisibleCount(getArtistVisibleCount());
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [type]);

  useEffect(() => {
    if (type !== 'ARTIST' || isLoading || !sectionKey) return;
    const entry = artistEntryRef.current;
    if (entry?.requestKey === sectionKey) {
      setArtistGroup({ requestKey: sectionKey, index: 0, side: entry.side, direction: entry.direction });
    }
    artistEntryRef.current = null;
  }, [isLoading, sectionKey, type]);

  const moveArtist = (direction) => {
    if (direction === 'next' && artistGroupIndex < artistGroupCount - 1) {
      setArtistGroup({ requestKey: sectionKey, index: artistGroupIndex + 1, side: 'index', direction });
      return;
    }
    if (direction === 'previous' && artistGroupIndex > 0) {
      setArtistGroup({ requestKey: sectionKey, index: artistGroupIndex - 1, side: 'index', direction });
      return;
    }
    if (direction === 'next' && hasNext) {
      artistEntryRef.current = { requestKey: getSectionKey(keyword, type, currentPage + 1), side: 'start', direction };
      onPageChange(type, currentPage + 1);
    } else if (direction === 'previous' && currentPage > 1) {
      artistEntryRef.current = { requestKey: getSectionKey(keyword, type, currentPage - 1), side: 'end', direction };
      onPageChange(type, currentPage - 1);
    }
  };

  const handleSelect = (item) => {
    if (type === 'TRACK') {
      saveSelectedTrack({ trackId: item.spotifyId, title: item.title });
      saveTrackSearch({ keyword, page: currentPage });
      navigate(buildRoute.trackDetail(item.spotifyId, item.title), { state: { fromSearch } });
    } else if (type === 'ARTIST') {
      navigate(buildRoute.artistDetail(item.spotifyId, item.title), { state: { fromSearch } });
    } else {
      navigate(buildRoute.albumDetail(item.spotifyId, item.title), { state: { fromSearch } });
    }
  };

  return (
    <section aria-busy={isLoading} aria-labelledby={`catalog-heading-${type.toLowerCase()}`} className={`catalog-section catalog-section--${type.toLowerCase()}`}>
      <div className="catalog-section-heading">
        <h2 className="catalog-section-title" id={`catalog-heading-${type.toLowerCase()}`} tabIndex={-1}>{label}</h2>
        {!isLoading && !error && keyword ? <span className="catalog-result-count">{totalCount}</span> : null}
      </div>

      {keyword && isLoading && type === 'ARTIST' ? (
        <div className="catalog-artist-stage catalog-artist-stage--loading" role="status">
          <span className="visually-hidden">{t('search.loadingType', { type: label })}</span>
          <div aria-hidden="true" className="catalog-artist-list catalog-search-skeleton-artists" style={{ '--artist-columns': artistVisibleCount }}>
            {Array.from({ length: artistVisibleCount }, (_, index) => (
              <div className="catalog-artist-card" key={index}><span className="catalog-loading-image skeleton-block" /><span className="catalog-loading-name skeleton-block" /></div>
            ))}
          </div>
        </div>
      ) : null}
      {keyword && isLoading && type !== 'ARTIST' ? (
        <div className="track-list track-list--loading catalog-search-skeleton-rows" role="status">
          <span className="visually-hidden">{t('search.loadingType', { type: label })}</span>
          {[0, 1, 2].map((index) => (
            <div aria-hidden="true" className="track-card" key={index}><span className="track-image skeleton-block" /><span className="track-content"><span className="skeleton-block" /><span className="skeleton-block" /></span></div>
          ))}
        </div>
      ) : null}
      {keyword && !isLoading && error ? <p className="tracks-message error">{t('search.error', { type: label })}</p> : null}
      {keyword && !isLoading && !error && items.length === 0 ? <p className="tracks-message">{t('search.empty', { type: label })}</p> : null}

      {keyword && !isLoading && !error && items.length > 0 && type === 'ARTIST' ? (
        <div className="catalog-artist-stage">
          <span aria-live="polite" className="visually-hidden">{t('search.currentPage', { type: label, currentPage })}</span>
          <button aria-label={t('common:actions.previousPage')} className="catalog-artist-arrow catalog-artist-arrow--previous" disabled={currentPage <= 1 && artistGroupIndex === 0} onClick={() => moveArtist('previous')} type="button"><span aria-hidden="true" /></button>
          <div aria-label={label} className="catalog-artist-list" data-direction={artistDirection} key={`${sectionKey}:${artistVisibleCount}:${artistGroupIndex}`} style={{ '--artist-columns': artistVisibleCount }}>
            {visibleArtists.map((item, index) => (
              <button className="catalog-artist-card" key={item.spotifyId} onClick={() => handleSelect(item)} style={{ '--artist-delay': `${index * 65}ms` }} type="button">
                <CatalogImage artist item={item} t={t} />
                <span className="catalog-artist-name">{item.title}</span>
              </button>
            ))}
          </div>
          <button aria-label={t('common:actions.nextPage')} className="catalog-artist-arrow catalog-artist-arrow--next" disabled={!hasNext && artistGroupIndex >= artistGroupCount - 1} onClick={() => moveArtist('next')} type="button"><span aria-hidden="true" /></button>
        </div>
      ) : null}

      {keyword && !isLoading && !error && items.length > 0 && type !== 'ARTIST' ? (
        <div className="track-list">
          {items.map((item) => (
            <button className="track-card" key={item.spotifyId} onClick={() => handleSelect(item)} type="button">
              <CatalogImage item={item} t={t} />
              <span className="track-content">
                <span className="track-title">{item.title}</span>
                <span className="track-meta">
                  {item.artistName ? <span className="track-meta-row"><span className="track-meta-label">{t('metadata.artist')}</span><span className="track-meta-value">{item.artistName}</span></span> : null}
                  {type === 'TRACK' && item.albumName ? <span className="track-meta-row"><span className="track-meta-label">{t('metadata.album')}</span><span className="track-meta-value">{item.albumName}</span></span> : null}
                </span>
              </span>
            </button>
          ))}
        </div>
      ) : null}

      {keyword && !isLoading && (items.length > 0 || currentPage > 1) && type !== 'ARTIST' ? (
        <nav className="tracks-pagination" aria-label={t('search.pagination', { type: label })}>
          <button aria-label={t('common:actions.previousPage')} className="pagination-button" disabled={currentPage <= 1} onClick={() => onPageChange(type, currentPage - 1)} type="button">←</button>
          <span aria-current="page" className="pagination-number active">{currentPage}</span>
          <button aria-label={t('common:actions.nextPage')} className="pagination-button" disabled={!hasNext || Boolean(error)} onClick={() => onPageChange(type, currentPage + 1)} type="button">→</button>
        </nav>
      ) : null}
    </section>
  );
}

export default function TracksPage() {
  const { t } = useTranslation('tracks');
  const [searchParams, setSearchParams] = useSearchParams();
  const inputKeyword = searchParams.get('keyword') ?? '';
  const keyword = inputKeyword.trim();
  const trackPage = getValidPage(searchParams.get('page'));
  const albumPage = getValidPage(searchParams.get('albumPage'));
  const artistPage = getValidPage(searchParams.get('artistPage'));
  const pages = { TRACK: trackPage, ALBUM: albumPage, ARTIST: artistPage };
  const requestKey = JSON.stringify([keyword, trackPage, albumPage, artistPage]);
  const fromSearch = `${routes.tracks}?${searchParams.toString()}`;
  const [request, setRequest] = useState({ key: null, keyword: null, pages: null, sections: {} });
  const requestRef = useRef(request);
  const isInitialLoading = Boolean(keyword) && request.keyword !== keyword;

  useEffect(() => {
    if (!keyword || requestRef.current.key === requestKey) return undefined;
    const controller = new AbortController();
    let ignore = false;
    const previous = requestRef.current;
    const targetPages = { TRACK: trackPage, ALBUM: albumPage, ARTIST: artistPage };
    const changedTypes = previous.keyword === keyword
      ? SEARCH_TYPES.filter((type) => targetPages[type] !== previous.pages[type])
      : SEARCH_TYPES;

    catalogSearchService.searchScreen({ keyword, trackPage, albumPage, artistPage, signal: controller.signal })
      .then((response) => {
        if (ignore) return;
        const sections = previous.keyword === keyword ? { ...previous.sections } : {};
        const committedPages = { ...targetPages };
        const emptyNextTypes = [];
        changedTypes.forEach((type) => {
          const result = response[RESPONSE_KEYS[type]];
          if (previous.keyword === keyword && targetPages[type] > previous.pages[type]
            && previous.sections[type]?.items.length > 0 && result.items.length === 0) {
            committedPages[type] = previous.pages[type];
            sections[type] = { ...previous.sections[type], hasNext: false };
            emptyNextTypes.push(type);
          } else {
            sections[type] = { items: result.items, totalCount: result.totalCount, hasNext: result.hasNext, error: null };
          }
        });
        const committedKey = JSON.stringify([keyword, committedPages.TRACK, committedPages.ALBUM, committedPages.ARTIST]);
        const next = { key: committedKey, keyword, pages: committedPages, sections };
        requestRef.current = next;
        setRequest(next);
        if (changedTypes.includes('TRACK') && !emptyNextTypes.includes('TRACK')) saveTrackSearch({ keyword, page: trackPage });
        if (emptyNextTypes.length > 0) {
          setSearchParams((current) => {
            const restored = new URLSearchParams(current);
            emptyNextTypes.forEach((type) => restored.set(PAGE_PARAM_KEYS[type], String(committedPages[type])));
            return restored;
          }, { replace: true });
        }
      })
      .catch((error) => {
        if (ignore) return;
        const sections = previous.keyword === keyword ? { ...previous.sections } : {};
        changedTypes.forEach((type) => { sections[type] = { ...EMPTY_SECTION, error }; });
        const next = { key: requestKey, keyword, pages: targetPages, sections };
        requestRef.current = next;
        setRequest(next);
      });

    return () => { ignore = true; controller.abort(); };
  }, [albumPage, artistPage, keyword, requestKey, setSearchParams, trackPage]);

  const updateKeyword = (nextKeyword) => {
    setSearchParams({ keyword: nextKeyword, page: '1', artistPage: '1', albumPage: '1' }, { replace: true });
  };

  const moveToPage = (type, page) => {
    const nextParams = new URLSearchParams(searchParams);
    nextParams.set(PAGE_PARAM_KEYS[type], String(page));
    setSearchParams(nextParams);
    const heading = document.getElementById(`catalog-heading-${type.toLowerCase()}`);
    heading?.focus({ preventScroll: true });
    heading?.scrollIntoView?.({ behavior: window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' });
  };

  return (
    <section className={keyword ? 'tracks-page tracks-page--has-keyword' : 'tracks-page'}>
      <header className="tracks-header">
        <h1 className="visually-hidden">{t('search.title')}</h1>
        <label className="visually-hidden" htmlFor="catalog-keyword">{t('search.keywordLabel')}</label>
        <input autoComplete="off" className="catalog-search-input" id="catalog-keyword" onChange={(event) => updateKeyword(event.target.value)} placeholder={t('search.keywordPlaceholder')} type="search" value={inputKeyword} />
        {!keyword ? <p className="tracks-description">{t('search.prompt')}</p> : null}
      </header>

      {isInitialLoading ? (
        <div className="catalog-page-loading track-loading" role="status">
          <p className="visually-hidden">{t('search.loadingAll')}</p>
          <span aria-hidden="true" className="catalog-loading-wave">
            {LOADING_ARTISTS.map((index) => (
              <span className="catalog-loading-artist" key={index}><span className="catalog-loading-image skeleton-block" /><span className="catalog-loading-name skeleton-block" /></span>
            ))}
          </span>
          <div aria-hidden="true" className="catalog-loading-results">
            {['TRACK', 'ALBUM'].map((type) => (
              <div className="catalog-section" key={type}>
                <span className="skeleton-block catalog-loading-heading" />
                <div className="track-list catalog-search-skeleton-rows">
                  {[0, 1].map((index) => (
                    <div className="track-card" key={index}><span className="track-image skeleton-block" /><span className="track-content"><span className="skeleton-block" /><span className="skeleton-block" /></span></div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : null}

      <div aria-hidden={isInitialLoading} className={isInitialLoading ? 'catalog-sections catalog-sections--hidden' : 'catalog-sections'}>
        {SEARCH_TYPES.map((type) => (
          <CatalogResultsSection
            currentPage={pages[type]}
            fromSearch={fromSearch}
            isLoading={Boolean(keyword) && request.key !== requestKey && (request.keyword !== keyword || request.pages?.[type] !== pages[type])}
            key={type}
            keyword={keyword}
            onPageChange={moveToPage}
            section={request.keyword === keyword ? request.sections[type] ?? EMPTY_SECTION : EMPTY_SECTION}
            sectionKey={request.keyword === keyword ? getSectionKey(keyword, type, request.pages[type]) : null}
            type={type}
          />
        ))}
      </div>
    </section>
  );
}
