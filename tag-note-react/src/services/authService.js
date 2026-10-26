import { apiClient } from './apiClient.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export function getOAuthAuthorizationUrl(provider) {
  return new URL(`/oauth2/authorization/${provider}`, API_BASE_URL).toString();
}

export const authService = {
  reissueAccessToken() {
    return apiClient.reissueAccessToken();
  },

  reissueRefreshToken() {
    return apiClient.post('/api/user/reissue/refreshToken');
  },

  async logout() {
    return apiClient.post('/api/user/logout');
  },
};
