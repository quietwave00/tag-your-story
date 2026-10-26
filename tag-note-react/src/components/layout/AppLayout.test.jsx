import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import i18n from '../../i18n/index.js';
import { authService } from '../../services/authService.js';
import { authStore } from '../../store/authStore.js';
import { tokenStorage } from '../../utils/tokenStorage.js';
import LoginPage from '../../pages/LoginPage.jsx';
import AppLayout from './AppLayout.jsx';

vi.mock('../../features/notifications/NotificationCenter.jsx', () => ({ default: () => null }));

function renderLayout() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<p>Home destination</p>} />
          <Route path="/login" element={<LoginPage />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  localStorage.clear();
  await i18n.changeLanguage('en');
  authStore.setAccessToken('access-token');
  tokenStorage.setRefreshToken('refresh-token');
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('logout', () => {
  it.each([
    ['success', () => Promise.resolve(), null],
    ['failure', () => Promise.reject(new Error('offline')), /server session may still be active/i],
  ])('clears the header and storage after server %s', async (_, reply, failureMessage) => {
    vi.spyOn(authService, 'logout').mockImplementation(reply);
    renderLayout();

    fireEvent.click(screen.getByRole('button', { name: 'EXIT' }));

    await waitFor(() => expect(authStore.getState().isAuthenticated).toBe(false));
    expect(tokenStorage.getAccessToken()).toBeNull();
    expect(tokenStorage.getRefreshToken()).toBeNull();
    expect(screen.queryByRole('button', { name: 'EXIT' })).not.toBeInTheDocument();
    if (failureMessage) {
      expect(await screen.findByText(failureMessage)).toBeInTheDocument();
    } else {
      expect(await screen.findByText('Home destination')).toBeInTheDocument();
    }
  });
});
