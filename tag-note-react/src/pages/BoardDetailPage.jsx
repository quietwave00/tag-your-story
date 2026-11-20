import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';
import CommentSection from '../features/comments/CommentSection.jsx';
import { boardService } from '../services/boardService.js';
import { likeService } from '../services/likeService.js';
import { AuthExpiredError } from '../services/apiClient.js';
import { useAuthStore } from '../store/authStore.js';
import BackButton from '../components/common/BackButton.jsx';
import UserAvatar from '../components/common/UserAvatar.jsx';
import useDetailScrollTop from '../hooks/useDetailScrollTop.js';
import { buildRoute, routes } from '../utils/routes.js';
import { formatCreatedAt } from '../utils/formatDate.js';
import { getSelectedTrack } from '../utils/trackSearchStorage.js';
import '../styles/boardDetail.css';

function getUserTagNames(board) {
  return board?.userTagNameList?.nameList ?? [];
}

function getLikeStatus(response) {
  return Boolean(response?.isLiked ?? response?.liked);
}

export default function BoardDetailPage() {
  const { i18n, t } = useTranslation(['boards', 'common', 'auth']);
  const { boardId } = useParams();
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [board, setBoard] = useState(null);
  const [isWriter, setIsWriter] = useState(false);
  const [isLiked, setIsLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [isBoardLoading, setIsBoardLoading] = useState(true);
  const [isLikeLoading, setIsLikeLoading] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [boardError, setBoardError] = useState(null);
  const [likeError, setLikeError] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const userTagNames = useMemo(() => getUserTagNames(board), [board]);
  const selectedTrack = useMemo(() => getSelectedTrack(), []);
  const displayLiked = isAuthenticated && isLiked;
  useDetailScrollTop(boardId);

  const moveToLogin = (messageKey, hasDraft = false) => {
    if (hasDraft && !window.confirm(t('auth:loginRequired.discardDraft'))) {
      return;
    }
    navigate(routes.login, { state: { messageKey } });
  };

  const handleLoginRequired = (hasDraft = false, reason = 'comment') => {
    moveToLogin(reason === 'expired' ? 'loginRequired.expired' : 'loginRequired.comment', hasDraft);
  };

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
      navigate(buildRoute.trackDetail(selectedTrack.trackId, selectedTrack.title));
      return;
    }

    navigate(-1);
  };

  const handleLikeClick = async () => {
    if (!boardId || isLikeLoading) {
      return;
    }
    if (!isAuthenticated) {
      moveToLogin('loginRequired.like');
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
      if (error instanceof AuthExpiredError) {
        moveToLogin('loginRequired.expired');
      } else {
        setLikeError(error);
      }
    } finally {
      setIsLikeLoading(false);
    }
  };

  const handleEdit = () => {
    if (!isAuthenticated) {
      moveToLogin('loginRequired.editBoard');
      return;
    }
    navigate(buildRoute.boardEdit(boardId));
  };

  const handleDelete = async () => {
    if (!boardId || isDeleting) {
      return;
    }
    if (!isAuthenticated) {
      moveToLogin('loginRequired.deleteBoard');
      return;
    }

    const shouldDelete = window.confirm(t('deleteConfirm'));

    if (!shouldDelete) {
      return;
    }

    setIsDeleting(true);

    try {
      await boardService.deleteBoard(boardId);
      handleBack();
    } catch (error) {
      if (error instanceof AuthExpiredError) {
        moveToLogin('loginRequired.expired');
      } else {
        setDeleteError(error);
      }
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="board-detail-page detail-page-frame">
      <BackButton
        label={selectedTrack.title ? t('detail.backToTrack', { title: selectedTrack.title }) : t('detail.back')}
        onClick={handleBack}
      />

      {isBoardLoading ? (
        <article className="board-detail-card board-detail-skeleton" role="status">
          <span className="visually-hidden">{t('detail.loading')}</span>
          <div aria-hidden="true" className="board-detail-meta">
            <div className="board-detail-byline"><span className="skeleton-block board-detail-skeleton-avatar" /><span className="board-detail-byline-text"><span className="skeleton-block" /><span className="skeleton-block" /></span></div>
            <span className="skeleton-block board-detail-skeleton-tags" />
          </div>
          <div aria-hidden="true" className="board-detail-content"><span className="skeleton-block" /><span className="skeleton-block" /><span className="skeleton-block" /></div>
          <div aria-hidden="true" className="board-detail-footer"><span className="skeleton-block" /></div>
        </article>
      ) : null}
      {!isBoardLoading && boardError ? (
        <p className="board-detail-message error">{t('detail.error')}</p>
      ) : null}

      {!isBoardLoading && !boardError && board ? (
        <>
          <div className="board-detail-layout">
            <article className="board-detail-card">
              <h1 className="visually-hidden">{t('detail.title')}</h1>
              <header className="board-detail-meta">
                <div className="board-detail-byline">
                  <UserAvatar name={board.nickname} size="large" />
                  <div className="board-detail-byline-text">
                    <strong className="board-detail-author">{board.nickname}</strong>
                    {board.createdAt ? (
                      <time className="board-detail-date">
                        {formatCreatedAt(board.createdAt, i18n.resolvedLanguage ?? i18n.language)}
                      </time>
                    ) : null}
                  </div>
                </div>
                {userTagNames.length > 0 ? (
                  <div className="board-detail-user-tags">
                    {userTagNames.map((userTag) => (
                      <span className="board-detail-user-tag" key={userTag}>
                        #{userTag}
                      </span>
                    ))}
                  </div>
                ) : null}
              </header>
              <p className="board-detail-content">{board.content}</p>
              <footer className="board-detail-footer">
                <section className="board-like-section" aria-label={t('detail.likeLabel')}>
                  <button
                    aria-label={t('detail.likeLabel')}
                    aria-pressed={displayLiked}
                    className={displayLiked ? 'board-like-button liked' : 'board-like-button'}
                    disabled={isLikeLoading}
                    onClick={handleLikeClick}
                    type="button"
                  >
                    {displayLiked ? '♥' : '♡'}
                  </button>
                  <span className="board-like-count">{t('detail.likeCount', { count: likeCount })}</span>
                </section>
                {isAuthenticated && isWriter ? (
                  <div className="board-detail-actions">
                    <button onClick={handleEdit} type="button">
                      {t('common:actions.edit')}
                    </button>
                    <button disabled={isDeleting} onClick={handleDelete} type="button">
                      {t('common:actions.delete')}
                    </button>
                  </div>
                ) : null}
              </footer>
              {deleteError ? <p className="board-detail-message error">{t('detail.deleteError')}</p> : null}
              {likeError ? <p className="board-detail-message error">{t('detail.likeError')}</p> : null}
            </article>

            <CommentSection boardId={boardId} isAuthenticated={isAuthenticated} onLoginRequired={handleLoginRequired} />
          </div>
        </>
      ) : null}
    </div>
  );
}
