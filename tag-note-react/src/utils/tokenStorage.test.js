import { beforeEach, describe, expect, it } from 'vitest';
import { tokenStorage } from './tokenStorage.js';

beforeEach(() => {
  localStorage.clear();
  ['Authorization', 'RefreshToken', 'Pending'].forEach((name) => {
    document.cookie = `${name}=; max-age=0; path=/`;
  });
});

describe('OAuth cookie synchronization', () => {
  it('stores both USER cookies without their Bearer prefixes and removes legacy pending state', () => {
    localStorage.setItem('Pending', 'old-pending');
    document.cookie = 'Pending=old-pending; path=/';
    document.cookie = 'Authorization=Bearer+access-token; path=/';
    document.cookie = 'RefreshToken=Bearer+refresh-token; path=/';

    expect(tokenStorage.syncFromCookies()).toEqual({ hasAccessToken: true });
    expect(tokenStorage.getAccessToken()).toBe('access-token');
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token');
    expect(tokenStorage.getPendingToken()).toBeNull();
  });

  it('does not accept a partial cookie pair or a pending cookie as a successful callback', () => {
    document.cookie = 'Pending=old-pending; path=/';
    document.cookie = 'Authorization=Bearer%20access-token; path=/';

    expect(tokenStorage.syncFromCookies()).toEqual({ hasAccessToken: false });
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getPendingToken()).toBeNull();
  });

  it('keeps logout cleared even if domain cookies remain readable', () => {
    document.cookie = 'Authorization=Bearer%20access-token; path=/';
    document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';
    tokenStorage.clearAuth();
    document.cookie = 'Authorization=Bearer%20access-token; path=/';
    document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';

    expect(tokenStorage.syncFromCookies()).toEqual({ hasAccessToken: false });
    expect(tokenStorage.getAccessToken()).toBeNull();

    tokenStorage.prepareForLogin();
    expect(tokenStorage.syncFromCookies()).toEqual({ hasAccessToken: true });
  });
});
