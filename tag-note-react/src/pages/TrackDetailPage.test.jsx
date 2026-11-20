import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { boardService } from '../services/boardService.js';
import { commentService } from '../services/commentService.js';
import { trackService } from '../services/trackService.js';
import { catalogSubjectTagsService } from '../services/catalogSubjectTagsService.js';
import { authStore } from '../store/authStore.js';
import TrackDetailPage from './TrackDetailPage.jsx';
import { getDetailBackTarget } from '../utils/detailNavigation.js';

function CurrentPath() {
  const location = useLocation();
  return <><p>Path: {location.pathname}</p><p>Return: {getDetailBackTarget(location)?.path}</p></>;
}

function renderTrack(path = '/track/track-1/track-title') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <CurrentPath />
      <Routes>
        <Route path="/track/:trackId" element={<TrackDetailPage />} />
        <Route path="/track/:trackId/:slug" element={<TrackDetailPage />} />
        <Route path="/artist/:spotifyArtistId/:slug" element={<p>Artist destination</p>} />
        <Route path="/album/:spotifyAlbumId/:slug" element={<p>Album destination</p>} />
        <Route path="/boards/new" element={<p>Board form destination</p>} />
        <Route path="/boards/:boardId" element={<p>Board destination</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  localStorage.clear();
  authStore.clear();
  await i18n.changeLanguage('en');
  vi.spyOn(trackService, 'getDetailTrackById').mockResolvedValue({ trackId: 'track-1', title: 'Track title', artistName: 'Artist', albumName: 'Album', spotifyArtistId: 'artist-1', spotifyAlbumId: 'album-1' });
  vi.spyOn(catalogSubjectTagsService, 'load').mockResolvedValue({ systemTags: [{ tagId: 1, name: 'Dream pop', score: 90 }], previewTags: [], tagDisplayStatus: 'CONFIRMED' });
  vi.spyOn(boardService, 'getBoardListByTrackId').mockResolvedValue({ boardResponseList: [], totalCount: 0 });
  vi.spyOn(boardService, 'getBoardByBoardId').mockResolvedValue({ likeCount: 3 });
  vi.spyOn(commentService, 'getCommentCountByBoardId').mockResolvedValue({ count: 2 });
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('track board entry', () => {
  it('shows artwork and note shaped skeletons while detail data is loading', () => {
    trackService.getDetailTrackById.mockReturnValue(new Promise(() => {}));
    boardService.getBoardListByTrackId.mockReturnValue(new Promise(() => {}));
    renderTrack();

    expect(document.querySelector('.track-detail-card .track-detail-image.skeleton-block')).toBeInTheDocument();
    expect(document.querySelectorAll('.board-list-skeleton .board-list-row')).toHaveLength(3);
    expect(screen.queryByRole('heading', { name: 'Track title' })).not.toBeInTheDocument();
  });

  it('loads confirmed tags for the track independently of its metadata', async () => {
    renderTrack();

    expect(await screen.findByText('#Dream pop')).toBeInTheDocument();
    const hero = screen.getByRole('heading', { name: 'Track title' }).closest('.track-detail-content');
    expect(within(hero).getByText('#Dream pop')).toBeInTheDocument();
    expect(within(hero).queryByRole('heading', { name: 'System tags' })).not.toBeInTheDocument();
    expect(catalogSubjectTagsService.load).toHaveBeenCalledWith('TRACK', 'track-1', expect.any(AbortSignal));
  });

  it('keeps track details visible when tag import fails', async () => {
    catalogSubjectTagsService.load.mockRejectedValue(new Error('Import failed'));
    renderTrack();

    expect(await screen.findByText('Track title')).toBeInTheDocument();
    expect(await screen.findByText('We could not load the tags.')).toHaveAttribute('role', 'alert');
    expect(await screen.findByText('No tag notes have been written yet.')).toBeInTheDocument();
  });

  it('opens the artist detail from its name and preserves the track return target', async () => {
    renderTrack();
    fireEvent.click(await screen.findByRole('link', { name: 'Artist' }));

    expect(await screen.findByText('Path: /artist/artist-1/artist')).toBeInTheDocument();
    expect(screen.getByText('Return: /track/track-1/track-title')).toBeInTheDocument();
  });

  it('opens the album detail from its name and preserves the track return target', async () => {
    renderTrack();
    fireEvent.click(await screen.findByRole('link', { name: 'Album' }));

    expect(await screen.findByText('Path: /album/album-1/album')).toBeInTheDocument();
    expect(screen.getByText('Return: /track/track-1/track-title')).toBeInTheDocument();
  });

  it('keeps names as text when the detail response has no destination IDs', async () => {
    trackService.getDetailTrackById.mockResolvedValue({ trackId: 'track-1', title: 'Track title', artistName: 'Artist', albumName: 'Album' });
    renderTrack();

    expect(await screen.findByText('Track title')).toBeInTheDocument();
    expect(screen.getByText('Artist')).toBeInTheDocument();
    expect(screen.getByText('Album')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Artist' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Album' })).not.toBeInTheDocument();
  });

  it('replaces an old ID-only URL with the canonical title slug', async () => {
    renderTrack('/track/track-1');
    expect(await screen.findByText('Path: /track/track-1/track-title')).toBeInTheDocument();
    expect(trackService.getDetailTrackById).toHaveBeenCalledWith('track-1');
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
  });

  it('shows a muted write action and a sign-in message to a guest', async () => {
    renderTrack();

    expect(await screen.findByText('Track title')).toBeInTheDocument();
    expect(await screen.findByText('No tag notes have been written yet.')).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Tag notes' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Tag notes' })).toBeInTheDocument();
    const writeButton = screen.getByRole('button', { name: 'WRITE A NOTE' });
    expect(writeButton).toHaveAttribute('aria-disabled', 'true');
    fireEvent.click(writeButton);
    expect(screen.getByText('Sign in to use this feature.')).toHaveAttribute('role', 'status');
    expect(screen.queryByText('Track ID')).not.toBeInTheDocument();
    expect(trackService.getDetailTrackById).toHaveBeenCalledWith('track-1');
    expect(boardService.getBoardListByTrackId).toHaveBeenCalledWith('track-1', 'CREATED_AT', 1);
  });

  it('shows existing tag notes to a guest', async () => {
    boardService.getBoardListByTrackId.mockResolvedValue({
      boardResponseList: [{ boardId: 12, content: 'A listener note', nickname: 'Listener', createdAt: [2026, 11, 2], userTagNameList: { nameList: ['dream pop'] } }],
      totalCount: 1,
    });
    renderTrack();

    const note = await screen.findByRole('button', { name: /A listener note/ });
    expect(note).toHaveClass('board-list-row');
    expect(screen.getByText('#dream pop')).toBeInTheDocument();
    expect(within(note).getByText('Listener')).toBeInTheDocument();
    expect(within(note).getByText('11/2/2026')).toBeInTheDocument();
    expect(await within(note).findByText('3 Likes')).toBeInTheDocument();
    expect(within(note).getByText('2 Comments')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Tag notes' })).getByRole('list').querySelectorAll('li')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'WRITE A NOTE' })).toHaveAttribute('aria-disabled', 'true');
  });

  it('opens a pared-down board form in a modal and saves a note', async () => {
    act(() => authStore.setAccessToken('access-token'));
    const create = vi.spyOn(boardService, 'writeBoard').mockResolvedValue({ boardId: 'board-1' });
    renderTrack();

    const writeButton = await screen.findByRole('button', { name: 'WRITE A NOTE' });
    fireEvent.click(writeButton);

    const dialog = await screen.findByRole('dialog', { name: 'Write a tag note' });
    expect(within(dialog).queryByRole('heading')).not.toBeInTheDocument();
    expect(within(dialog).queryByText('User tags')).not.toBeInTheDocument();
    expect(within(dialog).queryByText('Images')).not.toBeInTheDocument();
    expect(within(dialog).getByRole('textbox', { name: 'Story' })).not.toHaveAttribute('placeholder');

    fireEvent.change(within(dialog).getByRole('textbox', { name: 'Story' }), { target: { value: 'A listener note' } });
    fireEvent.change(within(dialog).getByRole('textbox', { name: 'Enter a tag and press Enter' }), { target: { value: 'shoegaze' } });
    fireEvent.keyDown(within(dialog).getByRole('textbox', { name: 'Enter a tag and press Enter' }), { key: 'Enter' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'WRITE' }));

    expect(await screen.findByText('Board destination')).toBeInTheDocument();
    expect(create).toHaveBeenCalledWith({ content: 'A listener note', userTagList: ['shoegaze'], trackId: 'track-1' });
  });

  it('closes the create modal with Escape and returns focus to the write button', async () => {
    act(() => authStore.setAccessToken('access-token'));
    renderTrack();

    const writeButton = await screen.findByRole('button', { name: 'WRITE A NOTE' });
    fireEvent.click(writeButton);
    fireEvent.keyDown(screen.getByRole('dialog', { name: 'Write a tag note' }), { key: 'Escape' });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(writeButton).toHaveFocus();
  });
});
