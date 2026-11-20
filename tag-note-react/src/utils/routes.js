import { toSlug } from './slug.js';

export const routes = {
  home: '/',
  genres: '/genres',
  login: '/login',
  signup: '/signup',
  myPage: '/my',
  token: '/token',
  nickname: '/nickname',
  tracks: '/search',
  tracksLegacy: '/tracks',
  contact: '/contact',
  trackDetail: '/track/:trackId/:slug',
  trackDetailLegacy: '/track/:trackId',
  artistDetail: '/artist/:spotifyArtistId/:slug',
  artistDetailLegacy: '/artist/:spotifyArtistId',
  albumDetail: '/album/:spotifyAlbumId/:slug',
  albumDetailLegacy: '/album/:spotifyAlbumId',
  boardDetail: '/boards/:boardId',
  boardEdit: '/boards/:boardId/edit',
  boardCreate: '/boards/new',
  exception: '/exception',
};

export const buildRoute = {
  trackDetail: (trackId, title) => `/track/${encodeURIComponent(trackId)}/${encodeURIComponent(toSlug(title))}`,
  artistDetail: (spotifyArtistId, name) => `/artist/${encodeURIComponent(spotifyArtistId)}/${encodeURIComponent(toSlug(name))}`,
  albumDetail: (spotifyAlbumId, title) => `/album/${encodeURIComponent(spotifyAlbumId)}/${encodeURIComponent(toSlug(title))}`,
  boardDetail: (boardId) => `/boards/${boardId}`,
  boardEdit: (boardId) => `/boards/${boardId}/edit`,
  boardCreate: (trackId) => `/boards/new?trackId=${encodeURIComponent(trackId)}`,
};
