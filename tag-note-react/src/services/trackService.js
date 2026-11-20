import { apiClient } from './apiClient.js';

function getDetail(trackId) {
  return apiClient.get(`/api/tracks/${trackId}`);
}

function getRanking() {
  return apiClient.get('/api/tracks/ranking');
}

export const trackService = {
  getDetail,

  getDetailTrackById(trackId) {
    return getDetail(trackId);
  },

  getRanking,

  getKeywordRanking() {
    return getRanking();
  },
};
