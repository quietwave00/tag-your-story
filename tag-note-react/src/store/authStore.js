import { createStore } from './createStore.js';
import { tokenStorage } from '../utils/tokenStorage.js';

function getInitialAuthState() {
  const accessToken = tokenStorage.getAccessToken();
  const pendingToken = tokenStorage.getPendingToken();

  return {
    accessToken,
    pendingToken,
    isAuthenticated: Boolean(accessToken),
    isPendingUser: Boolean(pendingToken && !accessToken),
  };
}

const store = createStore(getInitialAuthState());

export const authStore = {
  ...store,

  refreshFromStorage() {
    store.setState(getInitialAuthState());
  },

  clear() {
    tokenStorage.clearAuth();
    store.setState({
      accessToken: null,
      pendingToken: null,
      isAuthenticated: false,
      isPendingUser: false,
    });
  },

  clearPending() {
    tokenStorage.removePendingToken();
    store.setState(getInitialAuthState());
  },
};

export function useAuthStore(selector) {
  return authStore.useStore(selector);
}
