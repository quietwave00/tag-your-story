import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient, ApiError, AuthExpiredError } from './apiClient.js';
import { authStore } from '../store/authStore.js';
import { tokenStorage } from '../utils/tokenStorage.js';

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const expired = { exceptionCode: 'TOKEN_HAS_EXPIRED', message: 'Expired', status: 401 };

function setTokens() {
  tokenStorage.setRefreshToken('refresh');
  authStore.setAccessToken('old');
}

beforeEach(() => {
  authStore.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
  authStore.clear();
});

describe('apiClient response handling', () => {
  it('returns the success response and preserves empty and non JSON responses', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(json({ success: true, response: { id: 1 } }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response('ok', { status: 200 })));

    expect(await apiClient.get('/one')).toEqual({ id: 1 });
    expect(await apiClient.get('/two')).toBeNull();
    expect(await apiClient.get('/three')).toBeNull();
  });

  it.each([
    ['bare HTTP error', expired, 401],
    ['wrapped HTTP error', { success: false, response: expired }, 401],
    ['wrapped HTTP 200 error', { success: false, response: expired }, 200],
  ])('normalizes %s from the inner error', async (_, payload, status) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(json(payload, status)));

    await expect(apiClient.get('/protected', { withAuth: false })).rejects.toMatchObject({
      name: 'ApiError',
      message: 'Expired',
      exceptionCode: 'TOKEN_HAS_EXPIRED',
      status,
      logicalStatus: 401,
      payload,
    });
  });

  it('keeps the HTTP status when the logical status differs and does not reissue other errors', async () => {
    setTokens();
    const payload = { success: false, response: { exceptionCode: 'NO_PERMISSION', message: 'Denied', status: 403 } };
    const fetchMock = vi.fn().mockResolvedValue(json(payload));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/protected')).rejects.toMatchObject({
      status: 200, logicalStatus: 403, exceptionCode: 'NO_PERMISSION', message: 'Denied', payload,
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('keeps non JSON HTTP errors and network failures observable', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(new Response('failure', { status: 502 }))
      .mockRejectedValueOnce(new TypeError('offline')));

    await expect(apiClient.get('/one')).rejects.toMatchObject({ status: 502, message: 'HTTP 502', payload: null });
    await expect(apiClient.get('/two')).rejects.toThrow('offline');
  });
});

describe('access token reissue', () => {
  it('uses the refresh token only in the body, updates store, then retries once', async () => {
    setTokens();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json(expired, 401))
      .mockResolvedValueOnce(json({ success: true, response: { newAccessToken: 'new' } }))
      .mockResolvedValueOnce(json({ success: true, response: { id: 1 } }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/protected')).resolves.toEqual({ id: 1 });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[0][1].headers.get('Authorization')).toBe('old');
    expect(fetchMock.mock.calls[1][1].headers.has('Authorization')).toBe(false);
    expect(JSON.parse(fetchMock.mock.calls[1][1].body)).toEqual({ refreshToken: 'refresh' });
    expect(fetchMock.mock.calls[2][1].headers.get('Authorization')).toBe('new');
    expect(tokenStorage.getAccessToken()).toBe('new');
    expect(authStore.getState()).toMatchObject({ accessToken: 'new', isAuthenticated: true });
  });

  it('shares a pending reissue for simultaneous expired requests', async () => {
    setTokens();
    let releaseReissue;
    const pendingReissue = new Promise((resolve) => { releaseReissue = resolve; });
    const fetchMock = vi.fn((url) => {
      if (url.endsWith('/reissue/accessToken')) return pendingReissue;
      if (url.endsWith('/a') || url.endsWith('/b')) {
        const headers = fetchMock.mock.calls.at(-1)[1].headers;
        return Promise.resolve(headers.get('Authorization') === 'old'
          ? json(expired, 401) : json({ success: true, response: url }));
      }
      throw new Error(`Unexpected URL: ${url}`);
    });
    vi.stubGlobal('fetch', fetchMock);

    const a = apiClient.get('/a');
    const b = apiClient.get('/b');
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3));
    releaseReissue(json({ success: true, response: { newAccessToken: 'new' } }));
    await expect(Promise.all([a, b])).resolves.toHaveLength(2);
    expect(fetchMock.mock.calls.filter(([url]) => url.endsWith('/reissue/accessToken'))).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledTimes(5);
  });

  it('retries with a token another request already refreshed', async () => {
    setTokens();
    let releaseOldResponse;
    const oldResponse = new Promise((resolve) => { releaseOldResponse = resolve; });
    const fetchMock = vi.fn()
      .mockReturnValueOnce(oldResponse)
      .mockResolvedValueOnce(json({ success: true, response: { id: 1 } }));
    vi.stubGlobal('fetch', fetchMock);

    const result = apiClient.get('/protected');
    authStore.setAccessToken('new');
    releaseOldResponse(json(expired, 401));
    await expect(result).resolves.toEqual({ id: 1 });
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[1][1].headers.get('Authorization')).toBe('new');
  });

  it('does not retry a second expiration', async () => {
    setTokens();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json(expired, 401))
      .mockResolvedValueOnce(json({ success: true, response: { newAccessToken: 'new' } }))
      .mockResolvedValueOnce(json(expired, 401));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/protected')).rejects.toMatchObject({ exceptionCode: 'TOKEN_HAS_EXPIRED' });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(tokenStorage.getAccessToken()).toBe('new');
  });

  it('reissues for a wrapped HTTP 200 expiration', async () => {
    setTokens();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(json({ success: false, response: expired }))
      .mockResolvedValueOnce(json({ success: true, response: { newAccessToken: 'new' } }))
      .mockResolvedValueOnce(json({ success: true, response: { id: 1 } }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/protected')).resolves.toEqual({ id: 1 });
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it.each([
    ['wrapped response', () => json({ success: false, response: { exceptionCode: 'INVALID_REFRESH_TOKEN', message: 'Invalid', status: 401 } }, 401)],
    ['network failure', () => Promise.reject(new TypeError('offline'))],
    ['missing DTO field', () => json({ success: true, response: { accessToken: 'legacy' } })],
  ])('clears storage and store after reissue %s', async (_, reply) => {
    setTokens();
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(json(expired, 401))
      .mockImplementationOnce(reply));

    const error = await apiClient.get('/protected').catch((caught) => caught);
    expect(error).toBeInstanceOf(AuthExpiredError);
    if (_ === 'wrapped response') {
      expect(error).toMatchObject({ status: 401, logicalStatus: 401, exceptionCode: 'INVALID_REFRESH_TOKEN' });
    }
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(authStore.getState()).toMatchObject({ accessToken: null, isAuthenticated: false });
  });

  it('clears both states when the refresh token is absent', async () => {
    authStore.setAccessToken('old');
    const fetchMock = vi.fn().mockResolvedValue(json(expired, 401));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/protected')).rejects.toBeInstanceOf(AuthExpiredError);
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(authStore.getState().isAuthenticated).toBe(false);
  });

  it('does not reissue public requests or the reissue endpoint itself', async () => {
    setTokens();
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(json(expired, 401)));
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/public', { withAuth: false })).rejects.toBeInstanceOf(ApiError);
    await expect(apiClient.post('/api/user/reissue/accessToken', { refreshToken: 'refresh' })).rejects.toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[1][1].headers.has('Authorization')).toBe(false);
  });
});
