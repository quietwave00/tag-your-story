import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { boardService } from '../services/boardService.js';
import { fileService } from '../services/fileService.js';
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
  vi.useRealTimers();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('board form authentication', () => {
  it('edits inside the detail card and removes an existing tag on click', async () => {
    authStore.setAccessToken('access-token');
    vi.spyOn(boardService, 'getBoardByBoardId').mockResolvedValue({
      content: 'Existing story',
      nickname: 'Listener',
      createdAt: '2026-11-01T10:00:00',
      userTagNameList: { nameList: ['shoegaze'] },
    });
    vi.spyOn(fileService, 'getFileList').mockResolvedValue([
      { fileId: 'file-1', filePath: '/cover.jpg' },
    ]);
    renderForm('/boards/board-1/edit');

    expect(await screen.findByDisplayValue('Existing story')).toBeInTheDocument();
    expect(screen.getByText('Listener').closest('.board-detail-meta')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Story' }).closest('.board-detail-card')).toBeInTheDocument();
    expect(document.querySelector('.board-like-section')).not.toBeInTheDocument();
    expect(document.querySelector('.comment-section')).not.toBeInTheDocument();
    expect(document.querySelector('input[type="file"]')).not.toBeInTheDocument();
    expect(document.querySelector('.board-file-preview img')).toHaveAttribute('src', '/cover.jpg');

    fireEvent.click(screen.getByRole('button', { name: 'Remove tag shoegaze' }));
    expect(screen.queryByRole('button', { name: 'Remove tag shoegaze' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'EDIT' })).toBeDisabled();
  });

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

    fireEvent.change(screen.getByRole('textbox', { name: 'Story' }), { target: { value: 'My draft' } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Enter a tag and press Enter' }), { target: { value: 'shoegaze' } });
    fireEvent.keyDown(screen.getByRole('textbox', { name: 'Enter a tag and press Enter' }), { key: 'Enter' });
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

    fireEvent.change(screen.getByRole('textbox', { name: 'Story' }), { target: { value: 'Story' } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Enter a tag and press Enter' }), { target: { value: 'shoegaze' } });
    fireEvent.keyDown(screen.getByRole('textbox', { name: 'Enter a tag and press Enter' }), { key: 'Enter' });
    fireEvent.click(screen.getByRole('button', { name: 'WRITE' }));

    await waitFor(() => expect(create).toHaveBeenCalledWith({ content: 'Story', userTagList: ['shoegaze'], trackId: 'track-1' }));
    expect(await screen.findByText('Board destination')).toBeInTheDocument();
  });

  it('shows the five-tag warning in the tag list and removes it within two seconds', () => {
    authStore.setAccessToken('access-token');
    renderForm();
    const tagInput = screen.getByRole('textbox', { name: 'Enter a tag and press Enter' });

    for (let index = 1; index <= 5; index += 1) {
      fireEvent.change(tagInput, { target: { value: `tag${index}` } });
      fireEvent.keyDown(tagInput, { key: 'Enter' });
    }

    vi.useFakeTimers();
    fireEvent.change(tagInput, { target: { value: 'tag6' } });
    fireEvent.keyDown(tagInput, { key: 'Enter' });

    const warning = screen.getByRole('alert');
    expect(warning).toHaveTextContent('You can add up to five user tags.');
    expect(warning.closest('.board-edit-user-tag-list')).toBeInTheDocument();
    expect(document.querySelectorAll('.board-edit-user-tag')).toHaveLength(5);

    act(() => vi.advanceTimersByTime(1800));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('uses the same tag-list warning for empty and duplicate tags', () => {
    authStore.setAccessToken('access-token');
    renderForm();
    const tagInput = screen.getByRole('textbox', { name: 'Enter a tag and press Enter' });
    vi.useFakeTimers();

    fireEvent.keyDown(tagInput, { key: 'Enter' });
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a user tag.');
    expect(screen.getByRole('alert').closest('.board-edit-user-tag-list')).toBeInTheDocument();

    fireEvent.change(tagInput, { target: { value: 'shoegaze' } });
    fireEvent.keyDown(tagInput, { key: 'Enter' });
    fireEvent.change(tagInput, { target: { value: 'shoegaze' } });
    fireEvent.keyDown(tagInput, { key: 'Enter' });
    expect(screen.getByRole('alert')).toHaveTextContent('This user tag has already been added.');
    expect(screen.getByRole('alert').closest('.board-edit-user-tag-list')).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(1800));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
