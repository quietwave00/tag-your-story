import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { boardService } from '../services/boardService.js';
import { fileService } from '../services/fileService.js';
import { AuthExpiredError } from '../services/apiClient.js';
import { useAuthStore } from '../store/authStore.js';
import BackButton from '../components/common/BackButton.jsx';
import UserAvatar from '../components/common/UserAvatar.jsx';
import { formatCreatedAt } from '../utils/formatDate.js';
import { buildRoute, routes } from '../utils/routes.js';
import '../styles/boardDetail.css';
import '../styles/boardEdit.css';

const MAX_USER_TAG_COUNT = 5;

function normalizeUserTag(value) {
  return value.replace(/\s/g, '').replace(/^#/, '');
}

export default function BoardEditPage({ modal = false, onClose, trackIdOverride }) {
  const { i18n, t } = useTranslation(['boards', 'common', 'auth']);
  const { boardId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const wasAuthenticatedOnEntry = useRef(isAuthenticated);
  const dialogRef = useRef(null);
  const trackId = trackIdOverride ?? searchParams.get('trackId');
  const isEditMode = Boolean(boardId);
  const [content, setContent] = useState('');
  const [boardMeta, setBoardMeta] = useState(null);
  const [userTagInput, setUserTagInput] = useState('');
  const [userTags, setUserTags] = useState([]);
  const [existingFiles, setExistingFiles] = useState([]);
  const [deletedFileIds, setDeletedFileIds] = useState([]);
  const [isLoading, setIsLoading] = useState(isEditMode);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorKey, setErrorKey] = useState('');
  const [tagWarning, setTagWarning] = useState(null);

  const title = isEditMode ? t('form.editTitle') : t('form.createTitle');
  const canSubmit = content.trim() && userTags.length > 0 && !isSubmitting;

  useEffect(() => {
    if (!tagWarning) return undefined;
    const timeout = window.setTimeout(() => setTagWarning(null), 1800);
    return () => window.clearTimeout(timeout);
  }, [tagWarning]);

  const showTagWarning = (key) => {
    setTagWarning((current) => ({ key, id: (current?.id ?? 0) + 1 }));
    setErrorKey('');
  };

  useEffect(() => {
    if (!modal) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialogRef.current?.querySelector('textarea')?.focus();
    return () => { document.body.style.overflow = previousOverflow; };
  }, [modal]);

  const handleModalKeyDown = (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      onClose?.();
      return;
    }
    if (event.key !== 'Tab') return;
    const focusable = Array.from(dialogRef.current?.querySelectorAll('button:not(:disabled), input:not(:disabled), textarea:not(:disabled)') ?? []);
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  };

  useEffect(() => {
    if (!wasAuthenticatedOnEntry.current) {
      navigate(routes.login, { replace: true, state: { messageKey: isEditMode ? 'loginRequired.editBoard' : 'loginRequired.createBoard' } });
    } else if (!isAuthenticated && isEditMode && isLoading) {
      navigate(routes.login, { replace: true, state: { messageKey: 'loginRequired.expired' } });
    }
  }, [isAuthenticated, isEditMode, isLoading, navigate]);

  const moveToLogin = () => {
    const hasDraft = Boolean(content.trim() || userTags.length || deletedFileIds.length);
    if (hasDraft && !window.confirm(t('auth:loginRequired.discardDraft'))) return;
    navigate(routes.login, { state: { messageKey: 'loginRequired.expired' } });
  };

  useEffect(() => {
    if (!isAuthenticated || !isEditMode || !boardId) {
      return;
    }

    let ignore = false;

    async function loadBoardFormData() {
      setIsLoading(true);

      try {
        const [boardResponse, fileResponse] = await Promise.all([
          boardService.getBoardByBoardId(boardId),
          fileService.getFileList(boardId),
        ]);

        if (!ignore) {
          setBoardMeta(boardResponse);
          setContent(boardResponse?.content ?? '');
          setUserTags(boardResponse?.userTagNameList?.nameList ?? []);
          setExistingFiles(fileResponse ?? []);
          setErrorKey('');
        }
      } catch {
        if (!ignore) {
          setErrorKey('form.validation.loadFailed');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    loadBoardFormData();

    return () => {
      ignore = true;
    };
  }, [boardId, isAuthenticated, isEditMode]);

  const addUserTag = () => {
    const userTag = normalizeUserTag(userTagInput);

    if (!userTag) {
      showTagWarning('form.validation.tagRequired');
      return;
    }

    if (userTags.length >= MAX_USER_TAG_COUNT) {
      showTagWarning('form.validation.tagLimit');
      return;
    }

    if (userTags.includes(userTag)) {
      showTagWarning('form.validation.tagDuplicate');
      setUserTagInput('');
      return;
    }

    setUserTags((currentUserTags) => [...currentUserTags, userTag]);
    setTagWarning(null);
    setUserTagInput('');
    setErrorKey('');
  };

  const handleUserTagKeyDown = (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      addUserTag();
    }
  };

  const removeUserTag = (userTag) => {
    setUserTags((currentUserTags) => currentUserTags.filter((item) => item !== userTag));
    setTagWarning(null);
  };

  const removeExistingFile = (fileId) => {
    setExistingFiles((files) => files.filter((file) => file.fileId !== fileId));
    setDeletedFileIds((fileIds) => [...fileIds, fileId]);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!isAuthenticated) {
      moveToLogin();
      return;
    }

    if (!content.trim()) {
      setErrorKey('form.validation.contentRequired');
      return;
    }

    if (userTags.length === 0) {
      showTagWarning('form.validation.tagsRequired');
      return;
    }

    if (!isEditMode && !trackId) {
      setErrorKey('form.validation.trackMissing');
      return;
    }

    setIsSubmitting(true);

    try {
      if (isEditMode) {
        await boardService.updateBoardAndUserTag(boardId, content, userTags);

        if (deletedFileIds.length > 0) {
          await fileService.deleteFileList(deletedFileIds, boardId);
        }

        navigate(buildRoute.boardDetail(boardId));
        return;
      }

      const createdBoard = await boardService.writeBoard({
        content,
        userTagList: userTags,
        trackId,
      });

      navigate(buildRoute.boardDetail(createdBoard.boardId));
    } catch (error) {
      if (error instanceof AuthExpiredError) {
        moveToLogin();
      } else {
        setErrorKey('form.validation.saveFailed');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCancel = () => {
    if (modal) {
      onClose?.();
      return;
    }
    if (isEditMode) {
      navigate(buildRoute.boardDetail(boardId));
      return;
    }

    navigate(-1);
  };

  if (!wasAuthenticatedOnEntry.current) {
    return null;
  }

  if (isLoading) {
    return (
      <div className="board-detail-page detail-page-frame board-edit-detail-page">
        <div aria-hidden="true" className="skeleton-block board-edit-skeleton-back" />
        <div className="board-detail-card board-edit-detail-card board-edit-skeleton" role="status">
          <span className="visually-hidden">{t('form.loading')}</span>
          <div aria-hidden="true" className="board-detail-meta"><span className="skeleton-block board-edit-skeleton-author" /><span className="skeleton-block board-edit-skeleton-tags" /></div>
          <div aria-hidden="true" className="board-edit-detail-body"><span className="skeleton-block" /><span className="skeleton-block" /><span className="skeleton-block" /></div>
          <div aria-hidden="true" className="board-detail-footer"><span className="skeleton-block board-edit-skeleton-action" /></div>
        </div>
      </div>
    );
  }

  const tagEditor = (
    <section className={isEditMode ? 'board-edit-detail-tags' : 'board-edit-section'} aria-label={t('form.userTags')}>
        <div className="board-edit-user-tag-list">
          {userTags.map((userTag) => (
            <button
              aria-label={t('form.removeTag', { tag: userTag })}
              className="board-edit-user-tag"
              key={userTag}
              onClick={() => removeUserTag(userTag)}
              type="button"
            >
              #{userTag}
              {isEditMode ? <span aria-hidden="true" className="board-edit-tag-remove-mark">×</span> : null}
            </button>
          ))}
          {tagWarning ? (
            <span className="board-edit-tag-warning" key={tagWarning.id} role="alert">
              {t(tagWarning.key)}
            </span>
          ) : null}
        </div>
        <input
          className="board-edit-tag-input"
          onChange={(event) => setUserTagInput(event.target.value)}
          onKeyDown={handleUserTagKeyDown}
          aria-label={t('form.tagInput')}
          placeholder={t('form.tagInput')}
          type="text"
          value={userTagInput}
        />
    </section>
  );

  const imageEditor = existingFiles.length > 0 ? (
    <section className="board-edit-section board-edit-images" aria-label={t('form.images')}>
        <div className="board-file-preview-list">
          {existingFiles.map((file) => (
            <button
              aria-label={t('form.removeImage')}
              className="board-file-preview"
              key={file.fileId}
              onClick={() => removeExistingFile(file.fileId)}
              type="button"
            >
              <img src={file.filePath} alt={t('common:images.uploaded')} />
            </button>
          ))}
        </div>
    </section>
  ) : null;

  const contentEditor = (
    <section className={isEditMode ? 'board-edit-detail-body' : 'board-edit-section'} aria-label={t('form.story')}>
        <textarea
          aria-label={t('form.story')}
          className="board-edit-content"
          onChange={(event) => setContent(event.target.value)}
          placeholder={isEditMode ? t('form.storyPlaceholder') : undefined}
          value={content}
        />
    </section>
  );

  const feedback = (
    <>
      {errorKey ? <p className="board-edit-message error">{t(errorKey)}</p> : null}
      {!isAuthenticated ? <p className="board-edit-message error">{t('auth:loginRequired.expired')}</p> : null}
    </>
  );

  const actions = (
    <footer className={isEditMode ? 'board-detail-footer board-edit-detail-footer' : 'board-edit-footer'}>
        <button className="secondary-button" onClick={handleCancel} type="button">
          {t('common:actions.cancel')}
        </button>
        <button className="primary-button" disabled={!canSubmit} type="submit">
          {isEditMode ? t('common:actions.edit') : t('common:actions.write')}
        </button>
    </footer>
  );

  const form = isEditMode ? (
    <div className="board-detail-page detail-page-frame board-edit-detail-page">
      <BackButton label={t('detail.back')} onClick={handleCancel} />
      <form className="board-detail-card board-edit-detail-card" onSubmit={handleSubmit}>
        <h1 className="visually-hidden">{title}</h1>
        <header className="board-detail-meta board-edit-detail-meta">
          <div className="board-detail-byline">
            <UserAvatar name={boardMeta?.nickname} size="large" />
            <div className="board-detail-byline-text">
              <strong className="board-detail-author">{boardMeta?.nickname}</strong>
              {boardMeta?.createdAt ? (
                <time className="board-detail-date">
                  {formatCreatedAt(boardMeta.createdAt, i18n.resolvedLanguage ?? i18n.language)}
                </time>
              ) : null}
            </div>
          </div>
          {tagEditor}
        </header>
        {contentEditor}
        {imageEditor}
        {feedback}
        {actions}
      </form>
    </div>
  ) : (
    <form className="board-edit-page board-edit-page--create" onSubmit={handleSubmit}>
      {!modal ? <h1 className="visually-hidden">{title}</h1> : null}
      {tagEditor}
      {contentEditor}
      {feedback}
      {actions}
    </form>
  );

  if (!modal) return form;

  return createPortal(
    <div className="board-edit-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose?.(); }}>
      <div aria-label={title} aria-modal="true" className="board-edit-dialog" onKeyDown={handleModalKeyDown} ref={dialogRef} role="dialog">
        <button aria-label={t('common:actions.close')} className="board-edit-close" onClick={onClose} type="button">×</button>
        {form}
      </div>
    </div>,
    document.body,
  );
}
