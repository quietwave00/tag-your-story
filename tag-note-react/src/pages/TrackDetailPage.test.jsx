import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { boardService } from '../services/boardService.js';
import { trackService } from '../services/trackService.js';
import { authStore } from '../store/authStore.js';
import LoginPage from './LoginPage.jsx';
import TrackDetailPage from './TrackDetailPage.jsx';

function renderTrack() {
  return render(
    <MemoryRouter initialEntries={['/tracks/track-1']}>
      <Routes>
        <Route path="/tracks/:trackId" element={<TrackDetailPage />} />
        <Route path="/boards/new" element={<p>Board form destination</p>} />
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  localStorage.clear();
  authStore.clear();
  await i18n.changeLanguage('en');
  vi.spyOn(trackService, 'getDetailTrackById').mockResolvedValue({ title: 'Track title', artistName: 'Artist', albumName: 'Album' });
  vi.spyOn(boardService, 'getBoardListByTrackId').mockResolvedValue({ boardResponseList: [], totalCount: 0 });
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('track board entry', () => {
  it('keeps public reading available and guides a guest to login before opening the form', async () => {
    renderTrack();

    expect(await screen.findByText('Track title')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '+ ADD CUSTOM TAG' }));

    expect(await screen.findByText('Sign in to write a tag note.')).toBeInTheDocument();
    expect(trackService.getDetailTrackById).toHaveBeenCalledWith('track-1');
  });

  it('opens the existing board form for an authenticated user', async () => {
    renderTrack();

    const writeButton = await screen.findByRole('button', { name: '+ ADD CUSTOM TAG' });
    act(() => authStore.setAccessToken('access-token'));
    fireEvent.click(writeButton);

    expect(await screen.findByText('Board form destination')).toBeInTheDocument();
  });
});
