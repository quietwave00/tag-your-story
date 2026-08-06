import { deleteCookie, getCookie } from './cookie.js';

const TOKEN_KEYS = {
  accessToken: 'Authorization',
  refreshToken: 'RefreshToken',
  pendingToken: 'Pending',
};

function stripBearerPrefix(token) {
  return token?.replace(/^Bearer\+?/, '') ?? null;
}

function readStorage(key) {
  if (typeof localStorage === 'undefined') {
    return null;
  }

  return localStorage.getItem(key);
}

function writeStorage(key, value) {
  if (typeof localStorage === 'undefined' || !value) {
    return;
  }

  localStorage.setItem(key, value);
}

function removeStorage(key) {
  if (typeof localStorage === 'undefined') {
    return;
  }

  localStorage.removeItem(key);
}

export const tokenStorage = {
  keys: TOKEN_KEYS,

  getAccessToken() {
    return readStorage(TOKEN_KEYS.accessToken);
  },

  getRefreshToken() {
    return readStorage(TOKEN_KEYS.refreshToken);
  },

  getPendingToken() {
    return readStorage(TOKEN_KEYS.pendingToken);
  },

  setAccessToken(token) {
    writeStorage(TOKEN_KEYS.accessToken, stripBearerPrefix(token));
  },

  setRefreshToken(token) {
    writeStorage(TOKEN_KEYS.refreshToken, stripBearerPrefix(token));
  },

  setPendingToken(token) {
    writeStorage(TOKEN_KEYS.pendingToken, token);
  },

  removePendingToken() {
    removeStorage(TOKEN_KEYS.pendingToken);
    deleteCookie(TOKEN_KEYS.pendingToken);
  },

  syncFromCookies() {
    const accessToken = getCookie(TOKEN_KEYS.accessToken);
    const refreshToken = getCookie(TOKEN_KEYS.refreshToken);
    const pendingToken = getCookie(TOKEN_KEYS.pendingToken);

    if (accessToken) {
      this.setAccessToken(accessToken);
      this.removePendingToken();
    }

    if (refreshToken) {
      this.setRefreshToken(refreshToken);
    }

    if (pendingToken && !accessToken) {
      this.setPendingToken(pendingToken);
    }

    return {
      hasAccessToken: Boolean(accessToken),
      hasPendingToken: Boolean(pendingToken && !accessToken),
    };
  },

  clearAuth() {
    Object.values(TOKEN_KEYS).forEach((key) => {
      removeStorage(key);
      deleteCookie(key);
    });
  },
};
