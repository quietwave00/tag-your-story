import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { ApiError, AuthExpiredError } from '../services/apiClient.js';
import { boardService } from '../services/boardService.js';
import { likeService } from '../services/likeService.js';
import { authStore } from '../store/authStore.js';
import LoginPage from './LoginPage.jsx';
import BoardDetailPage from './BoardDetailPage.jsx';

vi.mock('../features/comments/CommentSection.jsx', () => ({ default: () => null }));

function BoardSource() {
  const navigate = useNavigate();
  return <button onClick={() => navigate('/boards/board-1')} type="button">Open board</button>;
}

function renderBoard(path = '/boards/board-1') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/source" element={<BoardSource />} />
        <Route path="/boards/:boardId" element={<BoardDetailPage />} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  localStorage.clear();
  authStore.clear();
  sessionStorage.clear();
  await i18n.changeLanguage('en');
  vi.spyOn(boardService, 'getBoardByBoardId').mockResolvedValue({ content: 'Public note', nickname: 'Listener', likeCount: 3 });
  vi.spyOn(boardService, 'isWriter').mockResolvedValue(true);
  vi.spyOn(likeService, 'checkLiked').mockResolvedValue({ isLiked: false });
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('board mutation entry', () => {
  it('resets scroll and shows the shared back button after an in-app transition', async () => {
    renderBoard('/source');
    fireEvent.click(screen.getByRole('button', { name: 'Open board' }));

    expect(await screen.findByText('Public note')).toBeInTheDocument();
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
    expect(screen.getByRole('button', { name: 'Go back' })).toHaveClass('detail-back-button');
  });

  it('keeps public board reading and redirects a guest like without sending a mutation', async () => {
    const like = vi.spyOn(likeService, 'like');
    renderBoard();

    expect(await screen.findByText('Public note')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Go back' })).toHaveClass('detail-back-button');
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
    fireEvent.click(screen.getByRole('button', { name: 'Like' }));

    expect(await screen.findByText('Sign in to like this tag note.')).toBeInTheDocument();
    expect(like).not.toHaveBeenCalled();
  });

  it('updates ownership controls from the auth store without blocking public reading', async () => {
    renderBoard();
    expect(await screen.findByText('Public note')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'EDIT' })).not.toBeInTheDocument();

    act(() => authStore.setAccessToken('access-token'));
    expect(await screen.findByRole('button', { name: 'EDIT' })).toBeInTheDocument();
    act(() => authStore.clear());
    await waitFor(() => expect(screen.queryByRole('button', { name: 'EDIT' })).not.toBeInTheDocument());
    expect(screen.getByText('Public note')).toBeInTheDocument();
  });

  it('keeps an authenticated like mutation and updates its displayed count', async () => {
    authStore.setAccessToken('access-token');
    const like = vi.spyOn(likeService, 'like').mockResolvedValue(true);
    renderBoard();

    fireEvent.click(await screen.findByRole('button', { name: 'Like' }));

    await waitFor(() => expect(like).toHaveBeenCalledWith('board-1'));
    expect(await screen.findByText('4 Likes')).toBeInTheDocument();
  });

  it('keeps permission errors on the board and routes expired auth to login', async () => {
    authStore.setAccessToken('access-token');
    const like = vi.spyOn(likeService, 'like')
      .mockRejectedValueOnce(new ApiError('Denied', { exceptionCode: 'NO_PERMISSION' }))
      .mockImplementationOnce(() => {
        authStore.clear();
        return Promise.reject(new AuthExpiredError());
      });
    renderBoard();

    fireEvent.click(await screen.findByRole('button', { name: 'Like' }));
    expect(await screen.findByText('We could not update the like.')).toBeInTheDocument();
    expect(screen.getByText('Public note')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Like' }));
    expect(await screen.findByText('Your sign-in has expired. Sign in again to continue.')).toBeInTheDocument();
    expect(like).toHaveBeenCalledTimes(2);
  });
});
