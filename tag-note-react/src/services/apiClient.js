import { tokenStorage } from '../utils/tokenStorage.js';
import { authStore } from '../store/authStore.js';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const AUTH_HEADER_NAME = import.meta.env.VITE_AUTH_HEADER_NAME ?? 'Authorization';
const TOKEN_EXPIRED_CODE = 'TOKEN_HAS_EXPIRED';
let reissuePromise = null;

export class ApiError extends Error {
  constructor(message, { status, logicalStatus, exceptionCode, payload } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.logicalStatus = logicalStatus;
    this.exceptionCode = exceptionCode;
    this.payload = payload;
  }
}

export class AuthExpiredError extends ApiError {
  constructor(cause) {
    super(cause?.message ?? 'Authentication has expired', {
      status: cause?.status,
      logicalStatus: cause?.logicalStatus,
      exceptionCode: cause?.exceptionCode ?? TOKEN_EXPIRED_CODE,
      payload: cause?.payload,
    });
    this.name = 'AuthExpiredError';
    if (cause) this.cause = cause;
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

function toApiError(response, payload) {
  const detail = payload?.success === false ? payload.response : payload;
  return new ApiError(detail?.message ?? `HTTP ${response.status}`, {
    status: response.status,
    logicalStatus: detail?.status,
    exceptionCode: detail?.exceptionCode,
    payload,
  });
}

async function parseBody(response) {
  const contentType = response.headers.get('content-type') ?? '';

  if (!contentType.includes('application/json')) {
    return null;
  }

  return response.json();
}

async function reissueAccessToken() {
  if (!reissuePromise) {
    const attempt = async () => {
      try {
        const refreshToken = tokenStorage.getRefreshToken();
        if (!refreshToken) throw new AuthExpiredError();

        const response = await fetch(buildUrl('/api/user/reissue/accessToken'), {
          method: 'POST',
          credentials: 'include',
          headers: new Headers({ 'Content-Type': 'application/json' }),
          body: JSON.stringify({ refreshToken }),
        });
        const payload = await parseBody(response);
        if (!response.ok || isApiException(payload)) throw toApiError(response, payload);

        const nextAccessToken = payload?.response?.newAccessToken;
        if (!nextAccessToken) {
          throw new ApiError('Access token was not returned', { status: response.status, payload });
        }

        authStore.setAccessToken(nextAccessToken);
        return nextAccessToken;
      } catch (error) {
        authStore.clear();
        throw new AuthExpiredError(error);
      }
    };
    reissuePromise = attempt().finally(() => { reissuePromise = null; });
  }
  return reissuePromise;
}

async function request(path, options = {}) {
  const { params, body, headers, withAuth = true, retryOnTokenExpired = true, ...fetchOptions } = options;
  const isReissueRequest = path === '/api/user/reissue/accessToken';
  const requestWithAuth = withAuth && !isReissueRequest;
  const isFormData = body instanceof FormData;
  const sentAccessToken = requestWithAuth ? tokenStorage.getAccessToken() : null;
  const response = await fetch(buildUrl(path, params), {
    credentials: 'include',
    ...fetchOptions,
    headers: buildHeaders(headers, body, { withAuth: requestWithAuth }),
    body: isFormData || typeof body === 'string' ? body : body ? JSON.stringify(body) : undefined,
  });
  const payload = await parseBody(response);

  const error = !response.ok || isApiException(payload) ? toApiError(response, payload) : null;

  if (error?.exceptionCode === TOKEN_EXPIRED_CODE && requestWithAuth && sentAccessToken && retryOnTokenExpired) {
    const currentAccessToken = tokenStorage.getAccessToken();
    if (!currentAccessToken || currentAccessToken === sentAccessToken) {
      await reissueAccessToken();
    }
    return request(path, { ...options, retryOnTokenExpired: false });
  }

  if (error) throw error;

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
