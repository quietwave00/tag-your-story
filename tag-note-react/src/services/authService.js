import { apiClient } from './apiClient.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export function getOAuthAuthorizationUrl(provider) {
  return new URL(`/oauth2/authorization/${provider}`, API_BASE_URL).toString();
}

export const authService = {
  loginWithEmail(email, password) {
    return apiClient.post('/api/auth/login', { email, password }, { withAuth: false });
  },

  startEmailSignup(email, password, passwordConfirm) {
    return apiClient.post('/api/auth/signup/email/start', { email, password, passwordConfirm }, { withAuth: false });
  },

  resendEmailSignup(challengeId) {
    return apiClient.post('/api/auth/signup/email/resend', { challengeId }, { withAuth: false });
  },

  confirmEmailSignup(challengeId, code) {
    return apiClient.post('/api/auth/signup/email/confirm', { challengeId, code }, { withAuth: false });
  },

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
