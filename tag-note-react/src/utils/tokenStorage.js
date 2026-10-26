import { deleteCookie, getCookie } from './cookie.js';

const TOKEN_KEYS = {
  accessToken: 'Authorization',
  refreshToken: 'RefreshToken',
  pendingToken: 'Pending',
};
const LOGGED_OUT_KEY = 'TagNoteLoggedOut';

function stripBearerPrefix(token) {
  return token?.replace(/^Bearer(?:\+|\s)+/i, '') ?? null;
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

  removePendingToken() {
    removeStorage(TOKEN_KEYS.pendingToken);
    deleteCookie(TOKEN_KEYS.pendingToken);
  },

  discardLegacyPending() {
    const hadPending = Boolean(readStorage(TOKEN_KEYS.pendingToken) || getCookie(TOKEN_KEYS.pendingToken));
    this.removePendingToken();
    return hadPending;
  },

  syncFromCookies() {
    this.discardLegacyPending();
    if (readStorage(LOGGED_OUT_KEY)) {
      return { hasAccessToken: false };
    }

    const accessToken = getCookie(TOKEN_KEYS.accessToken);
    const refreshToken = getCookie(TOKEN_KEYS.refreshToken);

    if (accessToken && refreshToken) {
      this.setAccessToken(accessToken);
      this.setRefreshToken(refreshToken);
    }

    return {
      hasAccessToken: Boolean(accessToken && refreshToken),
    };
  },

  prepareForLogin() {
    removeStorage(LOGGED_OUT_KEY);
  },

  clearAuth() {
    Object.values(TOKEN_KEYS).forEach((key) => {
      removeStorage(key);
      deleteCookie(key);
    });
    writeStorage(LOGGED_OUT_KEY, '1');
  },
};
