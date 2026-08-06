import { apiClient } from './apiClient.js';

function getDetail(trackId) {
  return apiClient.get(`/api/tracks/${trackId}`);
}

function getRanking() {
  return apiClient.get('/api/tracks/ranking');
}

export const trackService = {
  search({ keyword, page = 0 }) {
    return apiClient.get('/api/tracks', { params: { keyword, page } });
  },

  searchTrack(keyword, page = 1) {
    return apiClient.get('/api/tracks', { params: { keyword, page: page - 1 } });
  },

  getDetail,

  getDetailTrackById(trackId) {
    return getDetail(trackId);
  },

  getRanking,

  getKeywordRanking() {
    return getRanking();
  },
};
