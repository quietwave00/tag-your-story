import { apiClient } from './apiClient.js';

export const accountService = {
  getMe() {
    return apiClient.get('/api/user/me');
  },

  changeNickname(nickname) {
    return apiClient.patch('/api/user/me/nickname', { nickname });
  },

  withdraw() {
    return apiClient.delete('/api/user/me');
  },
};
