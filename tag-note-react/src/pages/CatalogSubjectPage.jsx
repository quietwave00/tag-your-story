import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import BackButton from '../components/common/BackButton.jsx';
import SubjectTags from '../components/catalog/SubjectTags.jsx';
import useDetailScrollTop from '../hooks/useDetailScrollTop.js';
import useArtworkAmbient from '../hooks/useArtworkAmbient.js';
import useSubjectTags from '../hooks/useSubjectTags.js';
import { catalogDetailService } from '../services/catalogDetailService.js';
import { buildRoute, routes } from '../utils/routes.js';
import { createDetailNavigationState, getDetailBackTarget } from '../utils/detailNavigation.js';
import { toSlug } from '../utils/slug.js';
import '../styles/catalogSubject.css';

const PAGE_SIZE = 10;
const RELEASE_TYPES = ['ALL', 'SINGLE_EP', 'ALBUM', 'COMPILATION'];
const INITIAL_RELEASE_PAGES = { ALL: 1, SINGLE_EP: 1, ALBUM: 1, COMPILATION: 1 };
const hasImage = (url) => url && url !== 'NO_IMAGE';
const hasReleaseType = (releaseType) => releaseType && releaseType !== 'ALL' && RELEASE_TYPES.includes(releaseType);

function waitForIndex(signal) {
  return new Promise((resolve, reject) => {
    const onAbort = () => { clearTimeout(timer); reject(new Error('Artist album request aborted')); };
    const timer = setTimeout(() => { signal.removeEventListener('abort', onAbort); resolve(); }, 1000);
    signal.addEventListener('abort', onAbort, { once: true });
    if (signal.aborted) onAbort();
  });
}

export default function CatalogSubjectPage({ type }) {
  const { t } = useTranslation(['tracks', 'common']);
  const navigate = useNavigate();
  const location = useLocation();
  const { spotifyArtistId, spotifyAlbumId, slug } = useParams();
  const spotifyId = type === 'ARTIST' ? spotifyArtistId : spotifyAlbumId;
  const listType = type === 'ARTIST' ? 'ALBUM' : 'TRACK';
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(true);
  const [detailError, setDetailError] = useState(false);
  const [releaseSelection, setReleaseSelection] = useState({ artistId: null, type: 'ALL', pages: INITIAL_RELEASE_PAGES });
  const [list, setList] = useState({ items: [], totalCount: 0, hasNext: false, classificationComplete: true, unclassifiedCount: 0 });
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState(false);
  const selectedReleaseType = releaseSelection.artistId === spotifyId ? releaseSelection.type : 'ALL';
  const listPage = releaseSelection.artistId === spotifyId ? releaseSelection.pages[selectedReleaseType] : 1;
  const totalPages = Math.max(1, Math.ceil(list.totalCount / PAGE_SIZE));
  const currentDetail = detail?.spotifyId === spotifyId ? detail : null;
  const subjectTags = useSubjectTags(type === 'ALBUM' ? 'ALBUM' : null, type === 'ALBUM' ? spotifyId : null);

  useDetailScrollTop(`${type}:${spotifyId}`);
  useArtworkAmbient(hasImage(currentDetail?.imageUrl) ? currentDetail.imageUrl : null);

  useEffect(() => {
    if (!spotifyId) return undefined;
    const controller = new AbortController();
    let active = true;
    setDetail(null);
    setDetailLoading(true);
    setDetailError(false);
    const getDetail = type === 'ARTIST' ? catalogDetailService.getArtist : catalogDetailService.getAlbum;
    getDetail(spotifyId, controller.signal)
      .then((response) => { if (active) setDetail(response); })
      .catch(() => { if (active) setDetailError(true); })
      .finally(() => { if (active) setDetailLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [spotifyId, type]);

  useEffect(() => {
    if (detail?.spotifyId !== spotifyId) return;
    const name = type === 'ARTIST' ? detail.name : detail.title;
    if (slug !== toSlug(name)) {
      navigate(type === 'ARTIST' ? buildRoute.artistDetail(spotifyId, name) : buildRoute.albumDetail(spotifyId, name), {
        replace: true,
        state: location.state,
      });
    }
  }, [detail, location.state, navigate, slug, spotifyId, type]);

  useEffect(() => {
    if (!spotifyId) return undefined;
    const controller = new AbortController();
    let active = true;
    setListLoading(true);
    setListError(false);

    const loadList = async () => {
      if (type !== 'ARTIST') {
        const response = await catalogDetailService.getAllAlbumTracks(spotifyId, controller.signal);
        return { ...response, hasNext: false, classificationComplete: true, unclassifiedCount: 0 };
      }
      while (active) {
        const response = await catalogDetailService.getArtistAlbums(spotifyId, listPage, selectedReleaseType, controller.signal);
        if (response.status !== 'BUILDING') {
          return { ...response, hasNext: listPage * PAGE_SIZE < response.totalCount };
        }
        await waitForIndex(controller.signal);
      }
      return null;
    };

    loadList()
      .then((response) => { if (active && response) setList(response); })
      .catch(() => { if (active) setListError(true); })
      .finally(() => { if (active) setListLoading(false); });
    return () => { active = false; controller.abort(); };
  }, [spotifyId, type, listPage, selectedReleaseType]);

  const selectReleaseType = (releaseType) => {
    setReleaseSelection((current) => ({
      artistId: spotifyId,
      type: releaseType,
      pages: current.artistId === spotifyId ? current.pages : INITIAL_RELEASE_PAGES,
    }));
  };

  const moveArtistPage = (nextPage) => {
    setReleaseSelection((current) => ({
      artistId: spotifyId,
      type: selectedReleaseType,
      pages: { ...(current.artistId === spotifyId ? current.pages : INITIAL_RELEASE_PAGES), [selectedReleaseType]: nextPage },
    }));
  };

  return (
    <div className="catalog-subject-page detail-page-frame" key={`${type}:${spotifyId}`}>
      <BackButton onClick={() => {
        const target = getDetailBackTarget(location);
        navigate(target?.path ?? routes.tracks, { state: target?.state });
      }} />
      <section aria-label={t(`search.sections.${type}`)} className={currentDetail && !detailLoading ? 'catalog-subject-hero catalog-subject-hero--ready' : 'catalog-subject-hero'}>
        {detailLoading ? (
          <>
            <span className="visually-hidden" role="status">{t('subject.loading', { type: t(`search.sections.${type}`) })}</span>
            <span aria-hidden="true" className={type === 'ARTIST' ? 'catalog-subject-artwork catalog-subject-artwork--artist skeleton-block' : 'catalog-subject-artwork skeleton-block'} />
            <div aria-hidden="true" className="catalog-subject-info catalog-subject-hero-skeleton">
              <span className="skeleton-block" />
              <span className="skeleton-block" />
              <span className="skeleton-block" />
            </div>
          </>
        ) : null}
        {!detailLoading && detailError ? <p role="alert">{t('subject.error', { type: t(`search.sections.${type}`) })}</p> : null}
        {currentDetail ? (
          <>
            <div className={type === 'ARTIST' ? 'catalog-subject-artwork catalog-subject-artwork--artist' : 'catalog-subject-artwork'}>
              {hasImage(currentDetail.imageUrl) ? <img alt={t('search.imageAlt', { title: type === 'ARTIST' ? currentDetail.name : currentDetail.title })} src={currentDetail.imageUrl} /> : <span>{t('search.noImage')}</span>}
            </div>
            <div className="catalog-subject-info">
              <p className="catalog-subject-kicker">{t(`search.sections.${type}`)}</p>
              <h1>{type === 'ARTIST' ? currentDetail.name : currentDetail.title}</h1>
              {type === 'ARTIST' ? (
                currentDetail.genres?.length > 0 ? <p className="catalog-subject-meta">{currentDetail.genres.join(' · ')}</p> : null
              ) : (
                <div className="catalog-subject-meta">
                  {currentDetail.artists?.length > 0 ? <p>{currentDetail.artists.map((artist, index) => (
                    <span key={artist.spotifyId}>{index > 0 ? ', ' : ''}<button className="catalog-subject-link" onClick={() => navigate(buildRoute.artistDetail(artist.spotifyId, artist.name), { state: createDetailNavigationState(location) })} type="button">{artist.name}</button></span>
                  ))}</p> : null}
                  {hasReleaseType(currentDetail.releaseType) ? <p>{t('subject.releaseTypeLabel')}: {t(`subject.releaseTypeNames.${currentDetail.releaseType}`)}</p> : null}
                  {currentDetail.releaseDate ? <p>{t('subject.releaseDate')}: {currentDetail.releaseDate}</p> : null}
                  {currentDetail.totalTracks != null ? <p>{t('subject.trackCount', { count: currentDetail.totalTracks })}</p> : null}
                </div>
              )}
            </div>
          </>
        ) : null}
      </section>
      {type === 'ALBUM' ? <SubjectTags {...subjectTags} /> : null}
      <section aria-labelledby="catalog-subject-list-title" className={!listLoading && !listError ? 'catalog-subject-list-section catalog-subject-list-section--ready' : 'catalog-subject-list-section'}>
        <h2 id="catalog-subject-list-title">{t(type === 'ARTIST' ? 'subject.albums' : 'subject.tracks')}</h2>
        {type === 'ARTIST' ? (
          <div aria-label={t('subject.releaseTypeLabel')} className="catalog-release-filters" role="group">
            {RELEASE_TYPES.map((releaseType) => (
              <button aria-pressed={selectedReleaseType === releaseType} className="catalog-release-filter" key={releaseType} onClick={() => selectReleaseType(releaseType)} type="button">
                {t(`subject.releaseTypes.${releaseType}`)}
              </button>
            ))}
          </div>
        ) : null}
        {listLoading ? (
          <div className="catalog-subject-list catalog-subject-list-skeleton" role="status">
            <span className="visually-hidden">{t('subject.listLoading', { type: t(`search.sections.${listType}`) })}</span>
            {[0, 1, 2, 3].map((index) => (
              <div aria-hidden="true" className="catalog-subject-row" key={index}>
                {type === 'ARTIST' ? <span className="skeleton-block catalog-subject-skeleton-image" /> : <span className="skeleton-block catalog-subject-skeleton-number" />}
                <span className="catalog-subject-row-text"><span className="skeleton-block" /><span className="skeleton-block" /></span>
                {type === 'ALBUM' ? <span className="skeleton-block catalog-subject-skeleton-duration" /> : null}
              </div>
            ))}
          </div>
        ) : null}
        {!listLoading && listError ? <p role="alert">{t('subject.listError', { type: t(`search.sections.${listType}`) })}</p> : null}
        {!listLoading && !listError && list.items.length === 0 ? <p>{t('subject.empty', { type: t(`search.sections.${listType}`) })}</p> : null}
        {!listLoading && !listError && list.items.length > 0 ? (
          <ol className="catalog-subject-list">
            {list.items.map((item, index) => (
              <li key={item.spotifyId}>
                {type === 'ARTIST' ? (
                  <button className="catalog-subject-row" onClick={() => navigate(buildRoute.albumDetail(item.spotifyId, item.title), { state: createDetailNavigationState(location) })} type="button">
                    {hasImage(item.imageUrl) ? <img alt="" src={item.imageUrl} /> : <span aria-hidden="true" className="catalog-subject-image-empty" />}
                    <span className="catalog-subject-row-text"><strong>{item.title}</strong>{item.releaseDate || hasReleaseType(item.releaseType) ? <small>{item.releaseDate}{item.releaseDate && hasReleaseType(item.releaseType) ? ' · ' : null}{hasReleaseType(item.releaseType) ? t(`subject.releaseTypeNames.${item.releaseType}`) : null}</small> : null}</span>
                  </button>
                ) : (
                  <button className="catalog-subject-row catalog-subject-row--track" onClick={() => navigate(buildRoute.trackDetail(item.spotifyId, item.title), { state: createDetailNavigationState(location) })} type="button">
                    <span className="catalog-subject-track-number">{index + 1}</span>
                    <span className="catalog-subject-row-text"><strong>{item.title}</strong>{item.artistName ? <small>{item.artistName}</small> : null}</span>
                    {item.durationMs != null ? <small>{t('subject.duration', { minutes: Math.floor(item.durationMs / 60000), seconds: String(Math.floor(item.durationMs / 1000) % 60).padStart(2, '0') })}</small> : null}
                  </button>
                )}
              </li>
            ))}
          </ol>
        ) : null}
        {type === 'ARTIST' && !listLoading && !listError && (listPage > 1 || list.hasNext) ? (
          <nav aria-label={t('subject.pagination', { type: t(`search.sections.${listType}`) })} className="catalog-subject-pagination">
            <button aria-label={t('common:actions.previousPage')} disabled={listPage <= 1} onClick={() => moveArtistPage(listPage - 1)} type="button">←</button>
            <span>{t('subject.pageStatus', { page: listPage, totalPages })}</span>
            <button aria-label={t('common:actions.nextPage')} disabled={!list.hasNext} onClick={() => moveArtistPage(listPage + 1)} type="button">→</button>
          </nav>
        ) : null}
      </section>
    </div>
  );
}
