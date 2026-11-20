import { createStore } from './createStore.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import { profileStorage } from '../utils/profileStorage.js';

function getInitialAuthState() {
  const accessToken = tokenStorage.getAccessToken();

  return {
    accessToken,
    isAuthenticated: Boolean(accessToken),
    nickname: accessToken ? profileStorage.getNickname() : null,
  };
}

const store = createStore(getInitialAuthState());

export const authStore = {
  ...store,

  setAccessToken(token) {
    tokenStorage.setAccessToken(token);
    store.setState(getInitialAuthState());
  },

  refreshFromStorage() {
    store.setState(getInitialAuthState());
  },

  setNickname(nickname) {
    profileStorage.setNickname(nickname);
    store.setState({ nickname: nickname || null });
  },

  clear() {
    tokenStorage.clearAuth();
    profileStorage.clear();
    store.setState({
      accessToken: null,
      isAuthenticated: false,
      nickname: null,
    });
  },
};

export function useAuthStore(selector) {
  return authStore.useStore(selector);
}
