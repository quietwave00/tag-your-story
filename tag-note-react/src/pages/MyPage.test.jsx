import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { accountService } from '../services/accountService.js';
import { likeService } from '../services/likeService.js';
import { authStore } from '../store/authStore.js';
import MyPage from './MyPage.jsx';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/my']}>
      <Routes>
        <Route path="/my" element={<MyPage />} />
        <Route path="/login" element={<p>Login destination</p>} />
        <Route path="/" element={<p>Home destination</p>} />
        <Route path="/boards/:boardId" element={<p>Board destination</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  authStore.clear();
  await i18n.changeLanguage('en');
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('my page', () => {
  it('sends guests to sign in', async () => {
    renderPage();
    expect(await screen.findByText('Login destination')).toBeInTheDocument();
  });

  it('shows a truthful tag state and loads liked boards using page one', async () => {
    authStore.setAccessToken('access-token');
    vi.spyOn(likeService, 'getMyLikedBoards').mockResolvedValue({ items: [{ boardId: 'board-1', author: 'Listener', content: 'A note' }], totalCount: 1 });
    renderPage();

    expect(screen.getByText('Your tag collection is coming soon.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Liked notes' }));
    expect(await screen.findByText('A note')).toBeInTheDocument();
    expect(likeService.getMyLikedBoards).toHaveBeenCalledWith(1);
    fireEvent.click(screen.getByRole('link', { name: /A note/ }));
    expect(screen.getByText('Board destination')).toBeInTheDocument();
  });

  it('requires a second action before account withdrawal', async () => {
    authStore.setAccessToken('access-token');
    vi.spyOn(accountService, 'withdraw').mockResolvedValue(null);
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Account' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete account' }));
    expect(accountService.withdraw).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(accountService.withdraw).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Delete account' }));
    fireEvent.click(within(screen.getByRole('group', { name: 'Delete your account?' })).getByRole('button', { name: 'Delete account' }));
    await waitFor(() => expect(accountService.withdraw).toHaveBeenCalledOnce());
    expect(await screen.findByText('Home destination')).toBeInTheDocument();
    expect(authStore.getState().isAuthenticated).toBe(false);
  });

  it('changes nickname through the account service', async () => {
    authStore.setAccessToken('access-token');
    authStore.setNickname('Listener');
    vi.spyOn(accountService, 'changeNickname').mockResolvedValue({ nickname: 'NewListener' });
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Settings' }));
    fireEvent.change(screen.getByLabelText('New nickname'), { target: { value: 'NewListener' } });
    fireEvent.click(screen.getByRole('button', { name: 'Change nickname' }));

    expect(await screen.findByText('Your nickname is now NewListener.')).toBeInTheDocument();
    expect(accountService.changeNickname).toHaveBeenCalledWith('NewListener');
    expect(screen.getByRole('heading', { name: 'NewListener' })).toBeInTheDocument();
    expect(authStore.getState().nickname).toBe('NewListener');
  });
});
