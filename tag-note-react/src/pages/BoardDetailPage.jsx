import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import CommentSection from '../features/comments/CommentSection.jsx';
import { boardService } from '../services/boardService.js';
import { likeService } from '../services/likeService.js';
import { buildRoute } from '../utils/routes.js';
import { getSelectedTrack } from '../utils/trackSearchStorage.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/boardDetail.css';

function getUserTagNames(board) {
  return board?.userTagNameList?.nameList ?? [];
}

function formatCreatedAt(createdAt) {
  if (!createdAt) {
    return '';
  }

  if (Array.isArray(createdAt)) {
    const [year, month, day] = createdAt;
    return `${year}.${month}.${day}`;
  }

  const date = new Date(createdAt);

  if (Number.isNaN(date.getTime())) {
    return String(createdAt);
  }

  return `${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()}`;
}

function getLikeStatus(response) {
  return Boolean(response?.isLiked ?? response?.liked);
}

export default function BoardDetailPage() {
  const { boardId } = useParams();
  const navigate = useNavigate();
  const [board, setBoard] = useState(null);
  const [isWriter, setIsWriter] = useState(false);
  const [isLiked, setIsLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [isBoardLoading, setIsBoardLoading] = useState(true);
  const [isLikeLoading, setIsLikeLoading] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [boardError, setBoardError] = useState(null);
  const [likeError, setLikeError] = useState(null);

  const userTagNames = useMemo(() => getUserTagNames(board), [board]);
  const selectedTrack = useMemo(() => getSelectedTrack(), []);
  const isAuthenticated = Boolean(tokenStorage.getAccessToken());

  useEffect(() => {
    if (!boardId) {
      return;
    }

    let ignore = false;

    async function loadBoardDetail() {
      setIsBoardLoading(true);

      try {
        const response = await boardService.getBoardByBoardId(boardId);

        if (!ignore) {
          setBoard(response);
          setLikeCount(response?.likeCount ?? 0);
          setBoardError(null);
        }
      } catch (error) {
        if (!ignore) {
          setBoard(null);
          setBoardError(error);
        }
      } finally {
        if (!ignore) {
          setIsBoardLoading(false);
        }
      }
    }

    loadBoardDetail();

    return () => {
      ignore = true;
    };
  }, [boardId]);

  useEffect(() => {
    if (!boardId || !isAuthenticated) {
      return;
    }

    let ignore = false;

    async function loadWriterStatus() {
      try {
        const response = await boardService.isWriter(boardId);

        if (!ignore) {
          setIsWriter(Boolean(response));
        }
      } catch {
        if (!ignore) {
          setIsWriter(false);
        }
      }
    }

    loadWriterStatus();

    return () => {
      ignore = true;
    };
  }, [boardId, isAuthenticated]);

  useEffect(() => {
    if (!boardId || !isAuthenticated) {
      return;
    }

    let ignore = false;

    async function loadLikeStatus() {
      try {
        const response = await likeService.checkLiked(boardId);

        if (!ignore) {
          setIsLiked(getLikeStatus(response));
          setLikeError(null);
        }
      } catch (error) {
        if (!ignore) {
          setLikeError(error);
        }
      }
    }

    loadLikeStatus();

    return () => {
      ignore = true;
    };
  }, [boardId, isAuthenticated]);

  const handleBack = () => {
    if (selectedTrack.trackId) {
      navigate(buildRoute.trackDetail(selectedTrack.trackId));
      return;
    }

    navigate(-1);
  };

  const handleLikeClick = async () => {
    if (!boardId || isLikeLoading) {
      return;
    }

    setIsLikeLoading(true);

    try {
      if (isLiked) {
        await likeService.cancelLike(boardId);
        setIsLiked(false);
        setLikeCount((count) => Math.max(count - 1, 0));
      } else {
        await likeService.like(boardId);
        setIsLiked(true);
        setLikeCount((count) => count + 1);
      }

      setLikeError(null);
    } catch (error) {
      setLikeError(error);
    } finally {
      setIsLikeLoading(false);
    }
  };

  const handleEdit = () => {
    navigate(buildRoute.boardEdit(boardId));
  };

  const handleDelete = async () => {
    if (!boardId || isDeleting) {
      return;
    }

    const shouldDelete = window.confirm('삭제하시겠습니까?');

    if (!shouldDelete) {
      return;
    }

    setIsDeleting(true);

    try {
      await boardService.deleteBoard(boardId);
      handleBack();
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="board-detail-page">
      <button className="board-detail-back" onClick={handleBack} type="button">
        <span>◀</span>
        {selectedTrack.title ? <span>{selectedTrack.title}으로 돌아가기</span> : <span>돌아가기</span>}
      </button>

      {isBoardLoading ? <p className="board-detail-message">게시글을 불러오는 중입니다.</p> : null}
      {boardError ? (
        <p className="board-detail-message error">게시글을 불러오지 못했습니다.</p>
      ) : null}

      {!isBoardLoading && !boardError && board ? (
        <>
          <article className="board-detail-card">
            <header className="board-detail-header">
              <div className="board-detail-user-tags">
                {userTagNames.map((userTag) => (
                  <button className="board-detail-user-tag" key={userTag} type="button">
                    #{userTag}
                  </button>
                ))}
              </div>
              <time className="board-detail-date">{formatCreatedAt(board.createdAt)}</time>
            </header>
            <p className="board-detail-content">{board.content}</p>
            <footer className="board-detail-author">{board.nickname}</footer>
          </article>

          {isWriter ? (
            <div className="board-detail-actions">
              <button onClick={handleEdit} type="button">
                수정
              </button>
              <button disabled={isDeleting} onClick={handleDelete} type="button">
                삭제
              </button>
            </div>
          ) : null}

          <section className="board-like-section" aria-label="좋아요">
            <button
              aria-pressed={isLiked}
              className={isLiked ? 'board-like-button liked' : 'board-like-button'}
              disabled={isLikeLoading}
              onClick={handleLikeClick}
              type="button"
            >
              {isLiked ? '♥' : '♡'}
            </button>
            <span className="board-like-count">{likeCount} Like</span>
          </section>
          {likeError ? <p className="board-detail-message error">좋아요 요청을 처리하지 못했습니다.</p> : null}

          <CommentSection boardId={boardId} isAuthenticated={isAuthenticated} />
        </>
      ) : null}
    </div>
  );
}
