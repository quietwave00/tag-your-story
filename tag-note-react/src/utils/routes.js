export const routes = {
  home: '/',
  login: '/login',
  token: '/token',
  nickname: '/nickname',
  tracks: '/tracks',
  contact: '/contact',
  trackDetail: '/tracks/:trackId',
  boardDetail: '/boards/:boardId',
  boardEdit: '/boards/:boardId/edit',
  boardCreate: '/boards/new',
  exception: '/exception',
};

export const buildRoute = {
  trackDetail: (trackId) => `/tracks/${trackId}`,
  boardDetail: (boardId) => `/boards/${boardId}`,
  boardEdit: (boardId) => `/boards/${boardId}/edit`,
  boardCreate: (trackId) => `/boards/new?trackId=${encodeURIComponent(trackId)}`,
};
