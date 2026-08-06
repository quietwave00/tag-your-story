import { apiClient } from './apiClient.js';

export const userTagService = {
  getRecent() {
    return apiClient.get('/api/board-user-tags/recent');
  },

  getRecentUserTagList() {
    return apiClient.get('/api/board-user-tags/recent');
  },
};
