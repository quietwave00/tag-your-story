import { createStore } from './createStore.js';
import { tokenStorage } from '../utils/tokenStorage.js';

function getInitialAuthState() {
  const accessToken = tokenStorage.getAccessToken();

  return {
    accessToken,
    isAuthenticated: Boolean(accessToken),
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

  clear() {
    tokenStorage.clearAuth();
    store.setState({
      accessToken: null,
      isAuthenticated: false,
    });
  },
};

export function useAuthStore(selector) {
  return authStore.useStore(selector);
}
