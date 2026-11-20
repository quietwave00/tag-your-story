import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { authStore } from '../store/authStore.js';
import i18n from '../i18n/index.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import { accountService } from '../services/accountService.js';
import TokenPage from './TokenPage.jsx';
import LoginPage from './LoginPage.jsx';
import NicknamePage from './NicknamePage.jsx';

function renderAt(path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/token" element={<TokenPage />} />
        <Route path="/nickname" element={<NicknamePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<p>Home destination</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  authStore.clear();
  localStorage.clear();
  await i18n.changeLanguage('en');
  ['Authorization', 'RefreshToken', 'Pending'].forEach((name) => {
    document.cookie = `${name}=; max-age=0; path=/`;
  });
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('OAuth 콜백', () => {
  it('로그인 후 서버에서 현재 닉네임을 읽어 이전 닉네임을 교체한다', async () => {
    authStore.setNickname('PreviousListener');
    vi.spyOn(accountService, 'getMe').mockResolvedValue({ nickname: 'GoogleListener' });
    document.cookie = 'Authorization=Bearer%20access-token; path=/';
    document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';

    renderAt('/token');

    expect(await screen.findByText('Home destination')).toBeInTheDocument();
    expect(authStore.getState()).toMatchObject({ accessToken: 'access-token', isAuthenticated: true });
    expect(authStore.getState().nickname).toBe('GoogleListener');
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token');
    expect(accountService.getMe).toHaveBeenCalledOnce();
  });

  it('프로필 조회가 실패해도 이전 계정의 닉네임을 표시하지 않는다', async () => {
    authStore.setNickname('PreviousListener');
    vi.spyOn(accountService, 'getMe').mockRejectedValue(new Error('offline'));
    document.cookie = 'Authorization=Bearer%20access-token; path=/';
    document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';

    renderAt('/token');

    expect(await screen.findByText('Home destination')).toBeInTheDocument();
    expect(authStore.getState().nickname).toBeNull();
  });

  it.each([
    ['GOOGLE_EMAIL_UNVERIFIED', /verify the email address/i],
    ['UNKNOWN_CODE', /sign-in failed/i],
  ])('오류 코드 %s가 있으면 쿠키가 있어도 안전한 오류를 표시한다', async (code, message) => {
    document.cookie = 'Authorization=Bearer%20access-token; path=/';
    document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';

    renderAt(`/token?error_code=${code}`);

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(authStore.getState().isAuthenticated).toBe(false);
  });

  it('인증 쿠키가 없으면 로그인 화면으로 이동한다', async () => {
    renderAt('/token');

    expect(await screen.findByText(/did not receive your sign-in details/i)).toBeInTheDocument();
    expect(authStore.getState().isAuthenticated).toBe(false);
  });

  it('예전 닉네임 경로를 열면 구글 로그인을 안내한다', async () => {
    localStorage.setItem('Pending', 'old-pending');
    renderAt('/nickname');

    expect(await screen.findByText(/previous sign-up flow has ended/i)).toBeInTheDocument();
    await waitFor(() => expect(localStorage.getItem('Pending')).toBeNull());
  });
});
