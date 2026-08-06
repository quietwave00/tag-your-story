import { apiClient } from './apiClient.js';
import { tokenStorage } from '../utils/tokenStorage.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const SSE_AUTH_MODE = import.meta.env.VITE_SSE_AUTH_MODE ?? 'query';

function buildSubscriptionUrl() {
  const url = new URL('/api/notification/subscription', API_BASE_URL);
  const token = tokenStorage.getAccessToken();

  if (SSE_AUTH_MODE === 'query' && token) {
    url.searchParams.set('Authorization', token);
  }

  return url.toString();
}

export const notificationService = {
  subscribe({ eventName = 'message', onMessage, onError } = {}) {
    const eventSource = new EventSource(buildSubscriptionUrl(), {
      withCredentials: SSE_AUTH_MODE === 'cookie',
    });

    if (onMessage) {
      eventSource.addEventListener(eventName, onMessage);
    }

    if (onError) {
      eventSource.addEventListener('error', onError);
    }

    return eventSource;
  },

  getList(page = 0) {
    return apiClient.get('/api/notification', { params: { page } });
  },

  getNotificationList(page = 0) {
    return apiClient.get('/api/notification', { params: { page } });
  },

  getCount() {
    return apiClient.get('/api/notification/count');
  },

  getNotificationCount() {
    return apiClient.get('/api/notification/count');
  },

  setAsRead(notificationId) {
    return apiClient.patch('/api/notification', { notificationId });
  },

  setAllAsRead() {
    return apiClient.patch('/api/notification/all');
  },
};
