import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import i18n from '../../i18n/index.js';
import { authService } from '../../services/authService.js';
import { authStore } from '../../store/authStore.js';
import { tokenStorage } from '../../utils/tokenStorage.js';
import LoginPage from '../../pages/LoginPage.jsx';
import MyPage from '../../pages/MyPage.jsx';
import AppLayout from './AppLayout.jsx';

vi.mock('../../features/notifications/NotificationCenter.jsx', () => ({ default: () => null }));

function renderLayout() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<p>Home destination</p>} />
          <Route path="/genres" element={<p>Genres destination</p>} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/my" element={<MyPage />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  localStorage.clear();
  await i18n.changeLanguage('en');
  authStore.setAccessToken('access-token');
  authStore.setNickname('Listener');
  tokenStorage.setRefreshToken('refresh-token');
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('account navigation', () => {
  it.each([
    ['success', () => Promise.resolve(), null],
    ['failure', () => Promise.reject(new Error('offline')), /server session may still be active/i],
  ])('moves logout into the nickname menu and clears storage after server %s', async (_, reply, failureMessage) => {
    vi.spyOn(authService, 'logout').mockImplementation(reply);
    renderLayout();

    expect(screen.queryByRole('button', { name: 'EXIT' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('link', { name: 'Listener' }));
    expect(screen.getByRole('heading', { name: 'Listener' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Account' }));
    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));

    await waitFor(() => expect(authStore.getState().isAuthenticated).toBe(false));
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(screen.queryByRole('link', { name: 'Listener' })).not.toBeInTheDocument();
    if (failureMessage) {
      expect(await screen.findByText(failureMessage)).toBeInTheDocument();
    } else {
      expect(await screen.findByText('Home destination')).toBeInTheDocument();
    }
  });
});

it('adds genre navigation without removing existing search navigation', () => {
  renderLayout();
  expect(screen.getByRole('link', { name: 'DIG' })).toHaveAttribute('href', '/search');
  fireEvent.click(screen.getByRole('link', { name: 'GENRES' }));
  expect(screen.getByText('Genres destination')).toBeInTheDocument();
});
