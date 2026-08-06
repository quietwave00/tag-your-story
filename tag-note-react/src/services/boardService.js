import { apiClient } from './apiClient.js';

function getListByTrackId({ trackId, orderType, page = 0 }) {
  return apiClient.get(`/api/boards/${trackId}`, {
    params: { 'order-type': orderType, page },
  });
}

function getDetail(boardId) {
  return apiClient.get('/api/boards', { params: { boardId } });
}

function getListByUserTagName(userTagName) {
  return apiClient.get('/api/boards/user-tags', { params: { userTagName } });
}

function checkWriter(boardId) {
  return apiClient.get(`/api/boards/auth/${boardId}`);
}

function deleteBoardRequest(boardId) {
  return apiClient.delete(`/api/boards/${boardId}`);
}

export const boardService = {
  create(payload) {
    return apiClient.post('/api/boards', payload);
  },

  writeBoard({ content, userTagList, trackId }) {
    return apiClient.post('/api/boards', { content, userTagList, trackId });
  },

  getListByTrackId,

  getBoardListByTrackId(trackId, orderType, page = 1) {
    return getListByTrackId({ trackId, orderType, page: page - 1 });
  },

  getDetail,

  getBoardByBoardId(boardId) {
    return getDetail(boardId);
  },

  getCountByTrackId(trackId) {
    return apiClient.get(`/api/boards/count/${trackId}`);
  },

  getListByUserTagName,

  checkWriter,

  isWriter(boardId) {
    return checkWriter(boardId);
  },

  update(payload) {
    return apiClient.patch('/api/boards', payload);
  },

  updateBoardAndUserTag(boardId, content, userTagList) {
    return apiClient.patch('/api/boards', { boardId, content, userTagList });
  },

  delete(boardId) {
    return deleteBoardRequest(boardId);
  },

  deleteBoard(boardId) {
    return deleteBoardRequest(boardId);
  },

  getBoardListByUserTagName(userTagName) {
    return getListByUserTagName(userTagName);
  },
};
