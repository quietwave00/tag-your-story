import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { boardService } from '../services/boardService.js';
import { trackService } from '../services/trackService.js';
import { buildRoute, routes } from '../utils/routes.js';
import { getTrackSearch } from '../utils/trackSearchStorage.js';
import '../styles/trackDetail.css';

const BOARD_PAGE_SIZE = 8;
const DEFAULT_PAGE = 1;
const DEFAULT_ORDER_TYPE = 'CREATED_AT';

const boardOrderOptions = [
  { label: '최신순', value: 'CREATED_AT' },
  { label: '추천순', value: 'LIKE' },
];

function getBoardPageNumbers(totalPages) {
  return Array.from({ length: totalPages }, (_, index) => index + 1);
}

function getUserTagNames(board) {
  return board?.userTagNameList?.nameList ?? [];
}

export default function TrackDetailPage() {
  const { trackId } = useParams();
  const navigate = useNavigate();
  const [track, setTrack] = useState(null);
  const [boards, setBoards] = useState([]);
  const [totalBoardCount, setTotalBoardCount] = useState(0);
  const [boardOrderType, setBoardOrderType] = useState(DEFAULT_ORDER_TYPE);
  const [boardPage, setBoardPage] = useState(DEFAULT_PAGE);
  const [isTrackLoading, setIsTrackLoading] = useState(true);
  const [isBoardLoading, setIsBoardLoading] = useState(true);
  const [trackError, setTrackError] = useState(null);
  const [boardError, setBoardError] = useState(null);

  const totalBoardPages = useMemo(() => {
    const calculatedTotalPages = Math.ceil(totalBoardCount / BOARD_PAGE_SIZE);

    return calculatedTotalPages < 1 ? 1 : calculatedTotalPages;
  }, [totalBoardCount]);

  const boardPageNumbers = useMemo(
    () => getBoardPageNumbers(totalBoardPages),
    [totalBoardPages],
  );

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
    if (!trackId) {
      return;
    }

    let ignore = false;

    async function loadBoardList() {
      setIsBoardLoading(true);

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
      navigate(buildRoute.boardCreate(trackId));
    }
  };

  return (
    <div className="track-detail-page">
      <button className="track-detail-back-button" onClick={handleBack} type="button">
        ◀
      </button>

      <section className="track-detail-card" aria-labelledby="track-detail-title">
        {isTrackLoading ? <p className="track-detail-message">트랙 정보를 불러오는 중입니다.</p> : null}
        {trackError ? (
          <p className="track-detail-message error">트랙 정보를 불러오지 못했습니다.</p>
        ) : null}
        {!isTrackLoading && !trackError && track ? (
          <>
            <img
              className="track-detail-image"
              src={track.imageUrl}
              alt={`${track.title} album cover`}
            />
            <div className="track-detail-content">
              <h1 className="track-detail-title" id="track-detail-title">
                {track.title}
              </h1>
              <dl className="track-detail-meta">
                <div className="track-detail-meta-row">
                  <dt>Artist</dt>
                  <dd>{track.artistName}</dd>
                </div>
                <div className="track-detail-meta-row">
                  <dt>Album</dt>
                  <dd>{track.albumName}</dd>
                </div>
                <div className="track-detail-meta-row">
                  <dt>Track ID</dt>
                  <dd>{trackId}</dd>
                </div>
              </dl>
              <button className="track-detail-write-button" onClick={moveBoardCreate} type="button">
                + ADD CUSTOM TAG
              </button>
            </div>
          </>
        ) : null}
      </section>

      <section className="board-list-section" aria-labelledby="board-list-title">
        <div className="board-list-header">
          <h2 className="board-list-title" id="board-list-title">
            게시글
          </h2>
          <fieldset className="board-order-fieldset">
            <legend className="visually-hidden">게시글 정렬</legend>
            {boardOrderOptions.map((option) => (
              <label className="board-order-option" key={option.value}>
                <input
                  checked={boardOrderType === option.value}
                  name="boardOrderType"
                  onChange={() => handleOrderChange(option.value)}
                  type="radio"
                  value={option.value}
                />
                <span>{option.label}</span>
              </label>
            ))}
          </fieldset>
        </div>

        {isBoardLoading ? <p className="track-detail-message">태그 노트를 불러오는 중입니다 █</p> : null}
        {boardError ? (
          <p className="track-detail-message error">태그 노트를 불러오지 못했습니다</p>
        ) : null}
        {!isBoardLoading && !boardError && boards.length === 0 ? (
          <p className="track-detail-message">아직 작성된 태그 노트가 없습니다</p>
        ) : null}

        <div className="board-list-grid">
          {boards.map((board) => (
            <button
              className="board-list-card"
              key={board.boardId}
              onClick={() => moveBoardDetail(board.boardId)}
              type="button"
            >
              <span className="board-user-tag-list">
                {getUserTagNames(board).map((userTag) => (
                  <span className="board-user-tag" key={`${board.boardId}-${userTag}`}>
                    #{userTag}
                  </span>
                ))}
              </span>
              <span className="board-content">{board.content}</span>
            </button>
          ))}
        </div>

        {!isBoardLoading && !boardError && boards.length > 0 ? (
          <nav className="board-pagination" aria-label="게시글 목록 페이지">
            <button
              aria-label="이전 페이지"
              className="board-pagination-button"
              disabled={boardPage <= 1}
              onClick={() => moveBoardPage(boardPage - 1)}
              type="button"
            >
              ⤺
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
              aria-label="다음 페이지"
              className="board-pagination-button"
              disabled={boardPage >= totalBoardPages}
              onClick={() => moveBoardPage(boardPage + 1)}
              type="button"
            >
              ⤻
            </button>
          </nav>
        ) : null}
      </section>
    </div>
  );
}
