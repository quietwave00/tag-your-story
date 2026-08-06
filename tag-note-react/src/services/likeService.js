import { apiClient } from './apiClient.js';

export const likeService = {
  like(boardId) {
    return apiClient.post('/api/likes', { boardId });
  },

  cancelLike(boardId) {
    return apiClient.delete('/api/likes', {
      body: { boardId },
    });
  },

  checkLiked(boardId) {
    return apiClient.get(`/api/likes/status/${boardId}`);
  },
};
