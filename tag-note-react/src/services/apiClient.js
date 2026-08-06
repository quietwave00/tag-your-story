import { tokenStorage } from '../utils/tokenStorage.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const AUTH_HEADER_NAME = import.meta.env.VITE_AUTH_HEADER_NAME ?? 'Authorization';
const TOKEN_EXPIRED_CODE = 'TOKEN_HAS_EXPIRED';

export class ApiError extends Error {
  constructor(message, { status, exceptionCode, payload } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.exceptionCode = exceptionCode;
    this.payload = payload;
  }
}

export class AuthExpiredError extends ApiError {
  constructor(payload) {
    super('Authentication has expired', {
      status: payload?.status,
      exceptionCode: TOKEN_EXPIRED_CODE,
      payload,
    });
    this.name = 'AuthExpiredError';
  }
}

function buildUrl(path, params) {
  const url = new URL(path, API_BASE_URL);

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, value);
      }
    });
  }

  return url.toString();
}

function buildHeaders(headers, body, { withAuth = true } = {}) {
  const nextHeaders = new Headers(headers);
  const token = tokenStorage.getAccessToken();

  if (withAuth && token && !nextHeaders.has(AUTH_HEADER_NAME)) {
    nextHeaders.set(AUTH_HEADER_NAME, token);
  }

  if (body && !(body instanceof FormData) && !nextHeaders.has('Content-Type')) {
    nextHeaders.set('Content-Type', 'application/json');
  }

  return nextHeaders;
}

function isApiException(payload) {
  return payload?.success === false || (payload?.exceptionCode && payload?.success !== true);
}

async function parseBody(response) {
  const contentType = response.headers.get('content-type') ?? '';

  if (!contentType.includes('application/json')) {
    return null;
  }

  return response.json();
}

async function reissueAccessToken() {
  const refreshToken = tokenStorage.getRefreshToken();

  if (!refreshToken) {
    throw new AuthExpiredError();
  }

  const response = await fetch(buildUrl('/api/user/reissue/accessToken'), {
    method: 'POST',
    credentials: 'include',
    headers: buildHeaders(
      {
        [AUTH_HEADER_NAME]: refreshToken,
        'Content-Type': 'application/json',
      },
      undefined,
      { withAuth: false },
    ),
    body: JSON.stringify({ refreshToken }),
  });
  const payload = await parseBody(response);

  if (!response.ok || isApiException(payload)) {
    if (payload?.exceptionCode === TOKEN_EXPIRED_CODE) {
      tokenStorage.clearAuth();
      throw new AuthExpiredError(payload);
    }

    throw new ApiError(payload?.message ?? `HTTP ${response.status}`, {
      status: response.status,
      exceptionCode: payload?.exceptionCode,
      payload,
    });
  }

  const nextAccessToken = payload?.response?.newAccessToken ?? payload?.response?.accessToken;

  if (!nextAccessToken) {
    throw new ApiError('Access token was not returned', { status: response.status, payload });
  }

  tokenStorage.setAccessToken(nextAccessToken);
  return nextAccessToken;
}

async function request(path, options = {}) {
  const { params, body, headers, withAuth = true, retryOnTokenExpired = true, ...fetchOptions } = options;
  const isFormData = body instanceof FormData;
  const response = await fetch(buildUrl(path, params), {
    credentials: 'include',
    ...fetchOptions,
    headers: buildHeaders(headers, body, { withAuth }),
    body: isFormData || typeof body === 'string' ? body : body ? JSON.stringify(body) : undefined,
  });
  const payload = await parseBody(response);

  if (payload?.exceptionCode === TOKEN_EXPIRED_CODE && retryOnTokenExpired) {
    await reissueAccessToken();
    return request(path, { ...options, retryOnTokenExpired: false });
  }

  if (!response.ok || isApiException(payload)) {
    throw new ApiError(payload?.message ?? `HTTP ${response.status}`, {
      status: response.status,
      exceptionCode: payload?.exceptionCode,
      payload,
    });
  }

  return payload?.response ?? payload;
}

export const apiClient = {
  get(path, options) {
    return request(path, { ...options, method: 'GET' });
  },

  post(path, body, options) {
    return request(path, { ...options, method: 'POST', body });
  },

  patch(path, body, options) {
    return request(path, { ...options, method: 'PATCH', body });
  },

  delete(path, options) {
    return request(path, { ...options, method: 'DELETE' });
  },

  reissueAccessToken,
};
