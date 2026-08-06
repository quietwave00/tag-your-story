import { apiClient } from './apiClient.js';

function deleteCommentRequest(commentId) {
  return apiClient.patch(`/api/comments/status/${commentId}`);
}

function getList({ boardId, page = 0 }) {
  return apiClient.get(`/api/comments/${boardId}/${page}`);
}

function getWritableCommentIds(boardId) {
  return apiClient.get(`/api/comments/auth/${boardId}`);
}

function getCount(boardId) {
  return apiClient.get(`/api/comments/count/${boardId}`);
}

function getReplies({ parentId, page = 0 }) {
  return apiClient.get(`/api/comments/replies/${parentId}/${page}`);
}

export const commentService = {
  create(payload) {
    return apiClient.post('/api/comments', payload);
  },

  writeComment(boardId, content) {
    return apiClient.post('/api/comments', { boardId, content });
  },

  update(payload) {
    return apiClient.patch('/api/comments', payload);
  },

  updateComment(commentId, content) {
    return apiClient.patch('/api/comments', { commentId, content });
  },

  delete(commentId) {
    return deleteCommentRequest(commentId);
  },

  deleteComment(commentId) {
    return deleteCommentRequest(commentId);
  },

  getList,

  getCommentList(boardId, page = 1) {
    return getList({ boardId, page: page - 1 });
  },

  getWritableCommentIds,

  getUserCommentId(boardId) {
    return getWritableCommentIds(boardId);
  },

  getCount,

  getCommentCountByBoardId(boardId) {
    return getCount(boardId);
  },

  createReply(payload) {
    return apiClient.post('/api/comments/replies', payload);
  },

  writeReply(boardId, parentId, content) {
    return apiClient.post('/api/comments/replies', { boardId, parentId, content });
  },

  getReplies,

  getReplyList(parentId, page = 0) {
    return getReplies({ parentId, page });
  },
};
