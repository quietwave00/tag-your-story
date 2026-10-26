import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { authStore } from '../store/authStore.js';
import i18n from '../i18n/index.js';
import { tokenStorage } from '../utils/tokenStorage.js';
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
  authStore.clear();
});

describe('OAuth callback', () => {
  it('accepts the new USER cookie pair and updates shared auth state', async () => {
    document.cookie = 'Authorization=Bearer%20access-token; path=/';
    document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';

    renderAt('/token');

    expect(await screen.findByText('Home destination')).toBeInTheDocument();
    expect(authStore.getState()).toMatchObject({ accessToken: 'access-token', isAuthenticated: true });
    expect(tokenStorage.getRefreshToken()).toBe('refresh-token');
  });

  it.each([
    ['GOOGLE_EMAIL_UNVERIFIED', /verify the email address/i],
    ['UNKNOWN_CODE', /sign-in failed/i],
  ])('shows a safe message for OAuth error %s even if cookies are present', async (code, message) => {
    document.cookie = 'Authorization=Bearer%20access-token; path=/';
    document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';

    renderAt(`/token?error_code=${code}`);

    expect(await screen.findByText(message)).toBeInTheDocument();
    expect(authStore.getState().isAuthenticated).toBe(false);
  });

  it('sends a cookie-free callback to login without authenticating', async () => {
    renderAt('/token');

    expect(await screen.findByText(/did not receive your sign-in details/i)).toBeInTheDocument();
    expect(authStore.getState().isAuthenticated).toBe(false);
  });

  it('sends a bookmarked legacy nickname route to Google login', async () => {
    localStorage.setItem('Pending', 'old-pending');
    renderAt('/nickname');

    expect(await screen.findByText(/previous sign-up flow has ended/i)).toBeInTheDocument();
    await waitFor(() => expect(localStorage.getItem('Pending')).toBeNull());
  });
});
