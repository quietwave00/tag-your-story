import { useCallback, useEffect, useMemo, useState } from 'react';
import { commentService } from '../../services/commentService.js';
import '../../styles/commentSection.css';

const COMMENT_PAGE_SIZE = 20;
const DEFAULT_PAGE = 1;
const INITIAL_REPLY_COUNT = 5;
const REPLY_COUNT_STEP = 5;

function getTotalPages(totalCount) {
  const totalPages = Math.ceil(totalCount / COMMENT_PAGE_SIZE);

  return totalPages < 1 ? 1 : totalPages;
}

function getPageNumbers(totalPages) {
  return Array.from({ length: totalPages }, (_, index) => index + 1);
}

function getWritableIdSet(writableCommentIds) {
  return new Set(writableCommentIds ?? []);
}

function CommentItem({
  comment,
  isReply = false,
  canEdit,
  onDelete,
  onEditStart,
  onReplyStart,
  children,
}) {
  return (
    <>
      <div className={isReply ? 'comment-row reply' : 'comment-row'}>
        <strong className="comment-nickname">{comment.nickname}</strong>
        <div className="comment-content">
          {isReply ? <span className="reply-symbol">↳ </span> : null}
          <span>{comment.content}</span>
          {!isReply ? (
            <button className="comment-reply-trigger" onClick={() => onReplyStart(comment.commentId)} type="button">
              ↳
            </button>
          ) : null}
        </div>
        {canEdit ? (
          <div className="comment-actions">
            <button onClick={() => onEditStart(comment)} type="button">
              수정
            </button>
            <button onClick={() => onDelete(comment.commentId)} type="button">
              삭제
            </button>
          </div>
        ) : null}
      </div>
      {children}
    </>
  );
}

export default function CommentSection({ boardId, isAuthenticated }) {
  const [comments, setComments] = useState([]);
  const [commentCount, setCommentCount] = useState(0);
  const [currentPage, setCurrentPage] = useState(DEFAULT_PAGE);
  const [newCommentContent, setNewCommentContent] = useState('');
  const [replyInputs, setReplyInputs] = useState({});
  const [activeReplyParentId, setActiveReplyParentId] = useState(null);
  const [visibleReplyCounts, setVisibleReplyCounts] = useState({});
  const [writableCommentIds, setWritableCommentIds] = useState([]);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingContent, setEditingContent] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const totalPages = useMemo(() => getTotalPages(commentCount), [commentCount]);
  const pageNumbers = useMemo(() => getPageNumbers(totalPages), [totalPages]);
  const writableIdSet = useMemo(() => getWritableIdSet(writableCommentIds), [writableCommentIds]);

  const loadWritableComments = useCallback(async () => {
    if (!boardId || !isAuthenticated) {
      setWritableCommentIds([]);
      return;
    }

    try {
      const response = await commentService.getUserCommentId(boardId);
      setWritableCommentIds(response ?? []);
    } catch {
      setWritableCommentIds([]);
    }
  }, [boardId, isAuthenticated]);

  const loadComments = useCallback(async () => {
    if (!boardId) {
      return;
    }

    setIsLoading(true);

    try {
      const [commentList, countResponse] = await Promise.all([
        commentService.getCommentList(boardId, currentPage),
        commentService.getCommentCountByBoardId(boardId),
      ]);

      setComments(commentList ?? []);
      setCommentCount(countResponse?.count ?? 0);
      setError(null);
    } catch (requestError) {
      setComments([]);
      setCommentCount(0);
      setError(requestError);
    } finally {
      setIsLoading(false);
    }
  }, [boardId, currentPage]);

  useEffect(() => {
    loadComments();
  }, [loadComments]);

  useEffect(() => {
    loadWritableComments();
  }, [loadWritableComments, comments]);

  const refreshComments = async (nextPage = currentPage) => {
    setCurrentPage(nextPage);

    if (nextPage === currentPage) {
      await loadComments();
      await loadWritableComments();
    }
  };

  const handleWriteComment = async (event) => {
    event.preventDefault();

    const content = newCommentContent.trim();

    if (!content || !boardId || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      await commentService.writeComment(boardId, content);
      setNewCommentContent('');
      await refreshComments(DEFAULT_PAGE);
    } catch (requestError) {
      setError(requestError);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCreateReply = async (parentId) => {
    const content = replyInputs[parentId]?.trim();

    if (!content || !boardId || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      await commentService.writeReply(boardId, parentId, content);
      setReplyInputs((inputs) => ({ ...inputs, [parentId]: '' }));
      setActiveReplyParentId(null);
      await refreshComments();
    } catch (requestError) {
      setError(requestError);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateComment = async (commentId) => {
    const content = editingContent.trim();

    if (!content || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      await commentService.updateComment(commentId, content);
      setEditingCommentId(null);
      setEditingContent('');
      await refreshComments();
    } catch (requestError) {
      setError(requestError);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId) => {
    const shouldDelete = window.confirm('삭제하시겠습니까?');

    if (!shouldDelete || isSubmitting) {
      return;
    }

    setIsSubmitting(true);

    try {
      await commentService.deleteComment(commentId);
      await refreshComments();
    } catch (requestError) {
      setError(requestError);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEditStart = (comment) => {
    setEditingCommentId(comment.commentId);
    setEditingContent(comment.content);
  };

  const handlePageChange = (page) => {
    if (page < 1 || page > totalPages || page === currentPage) {
      return;
    }

    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const showMoreReplies = (parentId) => {
    setVisibleReplyCounts((counts) => ({
      ...counts,
      [parentId]: (counts[parentId] ?? INITIAL_REPLY_COUNT) + REPLY_COUNT_STEP,
    }));
  };

  return (
    <section className="comment-section" aria-labelledby="comment-section-title">
      <h2 className="comment-section-title" id="comment-section-title">
        댓글
      </h2>

      <form className="comment-write-form" onSubmit={handleWriteComment}>
        <input
          className="comment-input"
          onChange={(event) => setNewCommentContent(event.target.value)}
          placeholder="Comment"
          type="text"
          value={newCommentContent}
        />
        <button className="comment-submit-button" disabled={!newCommentContent.trim() || isSubmitting} type="submit">
          WRITE
        </button>
      </form>

      {isLoading ? <p className="comment-message">댓글을 불러오는 중입니다.</p> : null}
      {error ? <p className="comment-message error">댓글 요청을 처리하지 못했습니다.</p> : null}
      {!isLoading && !error && comments.length === 0 ? (
        <p className="comment-message">작성된 댓글이 없습니다.</p>
      ) : null}

      <div className="comment-list">
        {comments.map(({ comment, children = [] }) => {
          const visibleReplyCount = visibleReplyCounts[comment.commentId] ?? INITIAL_REPLY_COUNT;
          const visibleReplies = children.slice(0, visibleReplyCount);
          const hasMoreReplies = children.length > visibleReplyCount;
          const isEditing = editingCommentId === comment.commentId;

          return (
            <div className="comment-thread" key={comment.commentId}>
              {isEditing ? (
                <div className="comment-edit-form">
                  <input
                    className="comment-edit-input"
                    onChange={(event) => setEditingContent(event.target.value)}
                    type="text"
                    value={editingContent}
                  />
                  <button disabled={!editingContent.trim() || isSubmitting} onClick={() => handleUpdateComment(comment.commentId)} type="button">
                    WRITE
                  </button>
                </div>
              ) : (
                <CommentItem
                  canEdit={writableIdSet.has(comment.commentId)}
                  comment={comment}
                  onDelete={handleDeleteComment}
                  onEditStart={handleEditStart}
                  onReplyStart={setActiveReplyParentId}
                />
              )}

              {activeReplyParentId === comment.commentId ? (
                <div className="reply-write-form">
                  <input
                    className="reply-input"
                    onChange={(event) =>
                      setReplyInputs((inputs) => ({
                        ...inputs,
                        [comment.commentId]: event.target.value,
                      }))
                    }
                    type="text"
                    value={replyInputs[comment.commentId] ?? ''}
                  />
                  <button
                    disabled={!replyInputs[comment.commentId]?.trim() || isSubmitting}
                    onClick={() => handleCreateReply(comment.commentId)}
                    type="button"
                  >
                    WRITE
                  </button>
                </div>
              ) : null}

              {visibleReplies.map((reply) => (
                editingCommentId === reply.commentId ? (
                  <div className="comment-edit-form reply" key={reply.commentId}>
                    <input
                      className="comment-edit-input"
                      onChange={(event) => setEditingContent(event.target.value)}
                      type="text"
                      value={editingContent}
                    />
                    <button disabled={!editingContent.trim() || isSubmitting} onClick={() => handleUpdateComment(reply.commentId)} type="button">
                      WRITE
                    </button>
                  </div>
                ) : (
                  <CommentItem
                    canEdit={writableIdSet.has(reply.commentId)}
                    comment={reply}
                    isReply
                    key={reply.commentId}
                    onDelete={handleDeleteComment}
                    onEditStart={handleEditStart}
                    onReplyStart={setActiveReplyParentId}
                  />
                )
              ))}

              {hasMoreReplies ? (
                <button className="more-replies-button" onClick={() => showMoreReplies(comment.commentId)} type="button">
                  ● ● ●
                </button>
              ) : null}
            </div>
          );
        })}
      </div>

      {!isLoading && !error && commentCount > 0 ? (
        <nav className="comment-pagination" aria-label="댓글 목록 페이지">
          <button
            aria-label="이전 페이지"
            disabled={currentPage <= 1}
            onClick={() => handlePageChange(currentPage - 1)}
            type="button"
          >
            ⤺
          </button>
          {pageNumbers.map((page) => (
            <button
              aria-current={page === currentPage ? 'page' : undefined}
              className={page === currentPage ? 'active' : undefined}
              key={page}
              onClick={() => handlePageChange(page)}
              type="button"
            >
              {page}
            </button>
          ))}
          <button
            aria-label="다음 페이지"
            disabled={currentPage >= totalPages}
            onClick={() => handlePageChange(currentPage + 1)}
            type="button"
          >
            ⤻
          </button>
        </nav>
      ) : null}
    </section>
  );
}
