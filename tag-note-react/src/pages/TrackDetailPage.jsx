import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import BackButton from '../components/common/BackButton.jsx';
import SubjectTags from '../components/catalog/SubjectTags.jsx';
import useDetailScrollTop from '../hooks/useDetailScrollTop.js';
import useArtworkAmbient from '../hooks/useArtworkAmbient.js';
import useSubjectTags from '../hooks/useSubjectTags.js';
import BoardEditPage from './BoardEditPage.jsx';
import { boardService } from '../services/boardService.js';
import { getBoardListMetrics } from '../features/boards/boardListMetrics.js';
import { trackService } from '../services/trackService.js';
import { useAuthStore } from '../store/authStore.js';
import { buildRoute, routes } from '../utils/routes.js';
import { toSlug } from '../utils/slug.js';
import { getTrackSearch } from '../utils/trackSearchStorage.js';
import { formatCreatedAt } from '../utils/formatDate.js';
import { createDetailNavigationState, getDetailBackTarget } from '../utils/detailNavigation.js';
import '../styles/trackDetail.css';

const BOARD_PAGE_SIZE = 8;
const DEFAULT_PAGE = 1;
const DEFAULT_ORDER_TYPE = 'CREATED_AT';

const boardOrderOptions = [
  { labelKey: 'boards.latest', value: 'CREATED_AT' },
  { labelKey: 'boards.popular', value: 'LIKE' },
];

function getBoardPageNumbers(totalPages) {
  return Array.from({ length: totalPages }, (_, index) => index + 1);
}

function getUserTagNames(board) {
  return board?.userTagNameList?.nameList ?? [];
}

export default function TrackDetailPage() {
  const { i18n, t } = useTranslation(['tracks', 'common']);
  const { trackId, slug } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [track, setTrack] = useState(null);
  const [boards, setBoards] = useState([]);
  const [totalBoardCount, setTotalBoardCount] = useState(0);
  const [boardOrderType, setBoardOrderType] = useState(DEFAULT_ORDER_TYPE);
  const [boardPage, setBoardPage] = useState(DEFAULT_PAGE);
  const [isTrackLoading, setIsTrackLoading] = useState(true);
  const [isBoardLoading, setIsBoardLoading] = useState(true);
  const [boardMetrics, setBoardMetrics] = useState({});
  const [trackError, setTrackError] = useState(null);
  const [boardError, setBoardError] = useState(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [writeNotice, setWriteNotice] = useState(false);
  const writeButtonRef = useRef(null);
  const subjectTags = useSubjectTags('TRACK', trackId);

  const totalBoardPages = useMemo(() => {
    const calculatedTotalPages = Math.ceil(totalBoardCount / BOARD_PAGE_SIZE);

    return calculatedTotalPages < 1 ? 1 : calculatedTotalPages;
  }, [totalBoardCount]);

  const boardPageNumbers = useMemo(
    () => getBoardPageNumbers(totalBoardPages),
    [totalBoardPages],
  );

  useDetailScrollTop(trackId);
  useArtworkAmbient(track?.trackId === trackId ? track.imageUrl : null);

  useEffect(() => {
    if (!trackId) {
      return;
    }

    let ignore = false;

    async function loadTrackDetail() {
      setIsTrackLoading(true);

      try {
        const response = await trackService.getDetailTrackById(trackId);

        if (!ignore) {
          setTrack(response);
          setTrackError(null);
        }
      } catch (error) {
        if (!ignore) {
          setTrack(null);
          setTrackError(error);
        }
      } finally {
        if (!ignore) {
          setIsTrackLoading(false);
        }
      }
    }

    loadTrackDetail();

    return () => {
      ignore = true;
    };
  }, [trackId]);

  useEffect(() => {
    if (track?.trackId === trackId && slug !== toSlug(track.title)) {
      navigate(buildRoute.trackDetail(trackId, track.title), { replace: true, state: location.state });
    }
  }, [location.state, navigate, slug, track, trackId]);

  useEffect(() => {
    if (!trackId) {
      return;
    }

    let ignore = false;

    async function loadBoardList() {
      setIsBoardLoading(true);
      setBoardMetrics({});

      try {
        const response = await boardService.getBoardListByTrackId(
          trackId,
          boardOrderType,
          boardPage,
        );

        if (!ignore) {
          setBoards(response?.boardResponseList ?? []);
          setTotalBoardCount(response?.totalCount ?? 0);
          setBoardError(null);
          getBoardListMetrics(response?.boardResponseList ?? [])
            .then((metrics) => {
              if (!ignore) setBoardMetrics(metrics);
            })
            .catch(() => {
              if (!ignore) setBoardMetrics({});
            });
        }
      } catch (error) {
        if (!ignore) {
          setBoards([]);
          setTotalBoardCount(0);
          setBoardError(error);
        }
      } finally {
        if (!ignore) {
          setIsBoardLoading(false);
        }
      }
    }

    loadBoardList();

    return () => {
      ignore = true;
    };
  }, [boardOrderType, boardPage, trackId]);

  const handleBack = () => {
    const target = getDetailBackTarget(location);
    if (target) {
      navigate(target.path, { state: target.state });
      return;
    }
    const { keyword, page } = getTrackSearch();

    if (keyword) {
      navigate(`${routes.tracks}?keyword=${encodeURIComponent(keyword)}&page=${page || DEFAULT_PAGE}`);
      return;
    }

    navigate(routes.tracks);
  };

  const handleOrderChange = (nextOrderType) => {
    setBoardOrderType(nextOrderType);
    setBoardPage(DEFAULT_PAGE);
  };

  const moveBoardPage = (page) => {
    if (page < 1 || page > totalBoardPages || page === boardPage) {
      return;
    }

    setBoardPage(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const moveBoardDetail = (boardId) => {
    navigate(buildRoute.boardDetail(boardId));
  };

  const moveBoardCreate = () => {
    if (trackId) {
      if (!isAuthenticated) {
        setWriteNotice(true);
        return;
      }
      setIsCreateOpen(true);
    }
  };

  const closeBoardCreate = () => {
    setIsCreateOpen(false);
    writeButtonRef.current?.focus();
  };

  return (
    <div className="track-detail-page detail-page-frame">
      <BackButton onClick={handleBack} />

      <section className="track-detail-card" aria-labelledby="track-detail-title">
        {isTrackLoading ? (
          <>
            <span className="visually-hidden" role="status">{t('detail.loading')}</span>
            <span aria-hidden="true" className="track-detail-image skeleton-block" />
            <div aria-hidden="true" className="track-detail-content track-detail-skeleton">
              <span className="skeleton-block" />
              <span className="skeleton-block" />
              <span className="skeleton-block" />
              <span className="skeleton-block" />
            </div>
          </>
        ) : null}
        {!isTrackLoading && trackError ? (
          <p className="track-detail-message error">{t('detail.error')}</p>
        ) : null}
        {!isTrackLoading && !trackError && track ? (
          <>
            <img
              className="track-detail-image"
              src={track.imageUrl}
              alt={t('common:images.albumCover', { title: track.title })}
            />
            <div className="track-detail-content">
              <SubjectTags {...subjectTags} hero />
              <h1 className="track-detail-title" id="track-detail-title">
                {track.title}
              </h1>
              <dl className="track-detail-meta">
                <div className="track-detail-meta-row">
                  <dt>{t('metadata.artist')}</dt>
                  <dd>{track.spotifyArtistId ? (
                    <Link className="track-detail-meta-link" state={createDetailNavigationState(location)} to={buildRoute.artistDetail(track.spotifyArtistId, track.artistName)}>{track.artistName}</Link>
                  ) : track.artistName}</dd>
                </div>
                <div className="track-detail-meta-row">
                  <dt>{t('metadata.album')}</dt>
                  <dd>{track.spotifyAlbumId ? (
                    <Link className="track-detail-meta-link" state={createDetailNavigationState(location)} to={buildRoute.albumDetail(track.spotifyAlbumId, track.albumName)}>{track.albumName}</Link>
                  ) : track.albumName}</dd>
                </div>
              </dl>
            </div>
          </>
        ) : null}
      </section>

      <section className="board-list-section" aria-labelledby="board-list-title">
        <div className="board-list-header">
          <div className="board-list-header-main">
            <h2 className="visually-hidden" id="board-list-title">{t('boards.title')}</h2>
            <button aria-disabled={!isAuthenticated} className={isAuthenticated ? 'track-detail-write-button' : 'track-detail-write-button track-detail-write-button--guest'} onClick={moveBoardCreate} ref={writeButtonRef} type="button">
              {t('boards.write')}
            </button>
          </div>
          {writeNotice && !isAuthenticated ? <p className="track-detail-write-notice" role="status">{t('boards.loginRequired')}</p> : null}
          <fieldset className="board-order-fieldset">
            <legend className="visually-hidden">{t('boards.sortLabel')}</legend>
            {boardOrderOptions.map((option) => (
              <label className="board-order-option" key={option.value}>
                <input
                  checked={boardOrderType === option.value}
                  name="boardOrderType"
                  onChange={() => handleOrderChange(option.value)}
                  type="radio"
                  value={option.value}
                />
                <span>{t(option.labelKey)}</span>
              </label>
            ))}
          </fieldset>
        </div>

        {isBoardLoading ? (
          <div className="board-list board-list-skeleton" role="status">
            <span className="visually-hidden">{t('boards.loading')}</span>
            {[0, 1, 2].map((index) => (
              <div aria-hidden="true" className="board-list-row" key={index}>
                <span className="skeleton-block" />
                <span className="skeleton-block" />
                <span className="skeleton-block" />
              </div>
            ))}
          </div>
        ) : null}
        {!isBoardLoading && boardError ? (
          <p className="track-detail-message error">{t('boards.error')}</p>
        ) : null}
        {!isBoardLoading && !boardError && boards.length === 0 ? (
          <p className="track-detail-message board-list-empty">{t('boards.empty')}</p>
        ) : null}

        {!isBoardLoading && boards.length > 0 ? (
          <ul className="board-list">
            {boards.map((board) => (
              <li key={board.boardId}>
                <button
                  className="board-list-row"
                  onClick={() => moveBoardDetail(board.boardId)}
                  type="button"
                >
                  {getUserTagNames(board).length > 0 ? (
                    <span className="board-user-tag-list">
                      {getUserTagNames(board).map((userTag) => (
                        <span className="board-user-tag" key={`${board.boardId}-${userTag}`}>
                          #{userTag}
                        </span>
                      ))}
                    </span>
                  ) : null}
                  <span className="board-content">{board.content}</span>
                  <span className="board-list-meta">
                    <span className="board-list-identity">
                      <span className="board-list-author">{board.nickname}</span>
                      {board.createdAt ? (
                        <time className="board-list-date">
                          {formatCreatedAt(board.createdAt, i18n.resolvedLanguage ?? i18n.language)}
                        </time>
                      ) : null}
                    </span>
                    <span className="board-list-engagement">
                      {boardMetrics[board.boardId]?.likeCount != null ? (
                        <span>{t('boards.likeCount', { count: boardMetrics[board.boardId].likeCount })}</span>
                      ) : null}
                      {boardMetrics[board.boardId]?.commentCount != null ? (
                        <span>{t('boards.commentCount', { count: boardMetrics[board.boardId].commentCount })}</span>
                      ) : null}
                    </span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ) : null}

        {!isBoardLoading && !boardError && boards.length > 0 ? (
          <nav className="board-pagination" aria-label={t('boards.pagination')}>
            <button
              aria-label={t('common:actions.previousPage')}
              className="board-pagination-button"
              disabled={boardPage <= 1}
              onClick={() => moveBoardPage(boardPage - 1)}
              type="button"
            >
              ←
            </button>
            {boardPageNumbers.map((page) => (
              <button
                aria-current={page === boardPage ? 'page' : undefined}
                className={page === boardPage ? 'board-page-number active' : 'board-page-number'}
                key={page}
                onClick={() => moveBoardPage(page)}
                type="button"
              >
                {page}
              </button>
            ))}
            <button
              aria-label={t('common:actions.nextPage')}
              className="board-pagination-button"
              disabled={boardPage >= totalBoardPages}
              onClick={() => moveBoardPage(boardPage + 1)}
              type="button"
            >
              →
            </button>
          </nav>
        ) : null}
      </section>
      {isCreateOpen ? <BoardEditPage modal onClose={closeBoardCreate} trackIdOverride={trackId} /> : null}
    </div>
  );
}
