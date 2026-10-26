import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { boardService } from '../services/boardService.js';
import { authStore } from '../store/authStore.js';
import LoginPage from './LoginPage.jsx';
import BoardEditPage from './BoardEditPage.jsx';

function renderForm(path = '/boards/new?trackId=track-1') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/boards/new" element={<BoardEditPage />} />
        <Route path="/boards/:boardId/edit" element={<BoardEditPage />} />
        <Route path="/boards/:boardId" element={<p>Board destination</p>} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  localStorage.clear();
  authStore.clear();
  await i18n.changeLanguage('en');
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('board form authentication', () => {
  it('redirects a direct guest visit before any board mutation', async () => {
    const create = vi.spyOn(boardService, 'writeBoard');
    renderForm();

    expect(await screen.findByText('Sign in to write a tag note.')).toBeInTheDocument();
    expect(create).not.toHaveBeenCalled();
  });

  it('redirects a direct guest edit visit before loading the form', async () => {
    const loadBoard = vi.spyOn(boardService, 'getBoardByBoardId');
    renderForm('/boards/board-1/edit');

    expect(await screen.findByText('Sign in to edit a tag note.')).toBeInTheDocument();
    expect(loadBoard).not.toHaveBeenCalled();
  });

  it('preserves a draft when auth changes and the user declines to leave', async () => {
    authStore.setAccessToken('access-token');
    const create = vi.spyOn(boardService, 'writeBoard');
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    renderForm();

    fireEvent.change(screen.getByPlaceholderText('Write your story.'), { target: { value: 'My draft' } });
    fireEvent.change(screen.getByPlaceholderText('Enter a tag and press Enter'), { target: { value: 'shoegaze' } });
    fireEvent.keyDown(screen.getByPlaceholderText('Enter a tag and press Enter'), { key: 'Enter' });
    act(() => authStore.clear());
    fireEvent.click(screen.getByRole('button', { name: 'WRITE' }));

    await waitFor(() => expect(window.confirm).toHaveBeenCalled());
    expect(screen.getByDisplayValue('My draft')).toBeInTheDocument();
    expect(create).not.toHaveBeenCalled();
  });

  it('keeps the authenticated create mutation', async () => {
    authStore.setAccessToken('access-token');
    const create = vi.spyOn(boardService, 'writeBoard').mockResolvedValue({ boardId: 'board-1' });
    renderForm();

    fireEvent.change(screen.getByPlaceholderText('Write your story.'), { target: { value: 'Story' } });
    fireEvent.change(screen.getByPlaceholderText('Enter a tag and press Enter'), { target: { value: 'shoegaze' } });
    fireEvent.keyDown(screen.getByPlaceholderText('Enter a tag and press Enter'), { key: 'Enter' });
    fireEvent.click(screen.getByRole('button', { name: 'WRITE' }));

    await waitFor(() => expect(create).toHaveBeenCalledWith({ content: 'Story', userTagList: ['shoegaze'], trackId: 'track-1' }));
    expect(await screen.findByText('Board destination')).toBeInTheDocument();
  });
});
