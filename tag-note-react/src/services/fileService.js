import { apiClient } from './apiClient.js';

export const fileService = {
  upload(formData) {
    return apiClient.post('/api/files', formData);
  },

  uploadFileList(formData) {
    return apiClient.post('/api/files', formData);
  },

  update(formData) {
    return apiClient.patch('/api/files', formData);
  },

  updateFileList(formData) {
    return apiClient.patch('/api/files', formData);
  },

  getMainFileList({ trackId, page = 0 }) {
    return apiClient.get(`/api/files/main/${trackId}`, { params: { page } });
  },

  getMainFileListByTrackId(trackId, page = 1) {
    return apiClient.get(`/api/files/main/${trackId}`, { params: { page: page - 1 } });
  },

  getFileList(boardId) {
    return apiClient.get(`/api/files/${boardId}`);
  },

  delete(payload) {
    return apiClient.delete('/api/files', { body: payload });
  },

  deleteFileList(fileIdList, boardId) {
    return apiClient.delete('/api/files', {
      body: { fileIdList, boardId },
    });
  },
};
