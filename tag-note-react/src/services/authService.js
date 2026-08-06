import { apiClient } from './apiClient.js';
import { tokenStorage } from '../utils/tokenStorage.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export function getOAuthAuthorizationUrl(provider) {
  return new URL(`/oauth2/authorization/${provider}`, API_BASE_URL).toString();
}

function register({ nickname }) {
  const pendingToken = tokenStorage.getPendingToken();

  return apiClient.post(
    '/api/user/register',
    { nickname },
    {
      headers: pendingToken ? { Authorization: pendingToken } : undefined,
    },
  );
}

export const authService = {
  reissueAccessToken() {
    return apiClient.reissueAccessToken();
  },

  reissueRefreshToken() {
    return apiClient.post('/api/user/reissue/refreshToken');
  },

  checkRegisterUser(pendingId = tokenStorage.getPendingToken()) {
    return apiClient.get('/api/user/check-registration', {
      headers: pendingId ? { Authorization: pendingId } : undefined,
    });
  },

  register,

  updateNickname(nickname) {
    return register({ nickname });
  },

  async logout() {
    return apiClient.post('/api/user/logout');
  },
};
