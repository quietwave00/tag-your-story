import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { catalogDetailService } from '../services/catalogDetailService.js';
import { catalogSubjectTagsService } from '../services/catalogSubjectTagsService.js';
import CatalogSubjectPage from './CatalogSubjectPage.jsx';

function CurrentPath() {
  const location = useLocation();
  return <p>Path: {location.pathname}</p>;
}

function renderSubject(path, type) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <CurrentPath />
      <Routes>
        <Route path="/artist/:spotifyArtistId" element={<CatalogSubjectPage type={type} />} />
        <Route path="/artist/:spotifyArtistId/:slug" element={<CatalogSubjectPage type={type} />} />
        <Route path="/album/:spotifyAlbumId" element={<CatalogSubjectPage type={type} />} />
        <Route path="/album/:spotifyAlbumId/:slug" element={<CatalogSubjectPage type={type} />} />
        <Route path="/search" element={<p>Search destination</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  await i18n.changeLanguage('en');
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
  vi.spyOn(catalogSubjectTagsService, 'load').mockResolvedValue({ systemTags: [], previewTags: [{ name: 'Ambient', source: 'LASTFM' }], tagDisplayStatus: 'PREVIEW' });
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); });

describe('Catalog subject pages', () => {
  it('shows artwork and row shaped skeletons while an artist loads', () => {
    vi.spyOn(catalogDetailService, 'getArtist').mockReturnValue(new Promise(() => {}));
    vi.spyOn(catalogDetailService, 'getArtistAlbums').mockReturnValue(new Promise(() => {}));
    renderSubject('/artist/artist-1', 'ARTIST');

    expect(document.querySelector('.catalog-subject-artwork--artist.skeleton-block')).toBeInTheDocument();
    expect(document.querySelectorAll('.catalog-subject-list-skeleton .catalog-subject-row')).toHaveLength(4);
    expect(screen.queryByRole('heading', { name: 'Example Artist' })).not.toBeInTheDocument();
  });

  it('loads artist details and pages through the artist album list', async () => {
    vi.spyOn(catalogDetailService, 'getArtist').mockResolvedValue({
      spotifyId: 'artist-1', name: 'Example Artist', imageUrl: 'NO_IMAGE', genres: ['Jazz'],
    });
    const getArtistAlbums = vi.spyOn(catalogDetailService, 'getArtistAlbums').mockImplementation((id, page) => Promise.resolve({
      items: Array.from({ length: page === 1 ? 10 : 2 }, (_, index) => ({ spotifyId: `album-${(page - 1) * 10 + index + 1}`, title: `Album ${(page - 1) * 10 + index + 1}`, imageUrl: 'NO_IMAGE', releaseDate: '2025', releaseType: 'ALBUM' })),
      totalCount: 12, classificationComplete: true,
    }));
    renderSubject('/artist/artist-1', 'ARTIST');

    expect(await screen.findByRole('heading', { name: 'Example Artist' })).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector('.catalog-subject-hero')).toHaveClass('catalog-subject-hero--ready'));
    expect(await screen.findByText('Path: /artist/artist-1/example-artist')).toBeInTheDocument();
    expect(screen.getByText('Jazz')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Album 1 2025 · Album' })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /^Album 1\b/ })).toBeInTheDocument();
    expect(getArtistAlbums).toHaveBeenCalledWith('artist-1', 1, 'ALL', expect.any(AbortSignal));
    expect(getArtistAlbums).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    expect(await screen.findByRole('button', { name: /^Album 11\b/ })).toBeInTheDocument();
    expect(getArtistAlbums).toHaveBeenCalledWith('artist-1', 2, 'ALL', expect.any(AbortSignal));
    expect(getArtistAlbums).toHaveBeenCalledTimes(2);
  });

  it('keeps an independent page for each release type using server classified pages', async () => {
    vi.spyOn(catalogDetailService, 'getArtist').mockResolvedValue({ spotifyId: 'artist-1', name: 'Example Artist', imageUrl: 'NO_IMAGE', genres: [] });
    const getArtistAlbums = vi.spyOn(catalogDetailService, 'getArtistAlbums').mockImplementation((id, page, releaseType) => Promise.resolve({
      items: Array.from({ length: page === 1 ? 10 : 1 }, (_, index) => ({
        spotifyId: `${releaseType.toLowerCase()}-${(page - 1) * 10 + index + 1}`,
        title: `${releaseType === 'SINGLE_EP' ? 'Single' : 'Album'} ${(page - 1) * 10 + index + 1}`,
        releaseType,
      })),
      totalCount: 11, classificationComplete: true,
    }));
    renderSubject('/artist/artist-1/example-artist', 'ARTIST');

    expect(await screen.findByRole('button', { name: 'Album 1 Album' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Singles & EPs' }));
    expect(await screen.findByRole('button', { name: 'Single 1 Single / EP' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    expect(await screen.findByRole('button', { name: 'Single 11 Single / EP' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Albums' }));
    expect(await screen.findByRole('button', { name: 'Album 1 Album' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
    expect(await screen.findByRole('button', { name: 'Album 11 Album' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Singles & EPs' }));
    expect(await screen.findByRole('button', { name: 'Single 11 Single / EP' })).toBeInTheDocument();
    expect(getArtistAlbums).toHaveBeenCalledTimes(6);
    expect(getArtistAlbums).toHaveBeenCalledWith('artist-1', 2, 'SINGLE_EP', expect.any(AbortSignal));
    expect(getArtistAlbums).toHaveBeenCalledWith('artist-1', 2, 'ALBUM', expect.any(AbortSignal));
  });

  it('loads album details and its track list independently', async () => {
    vi.spyOn(catalogDetailService, 'getAlbum').mockResolvedValue({
      spotifyId: 'album-1', title: 'Example Album', imageUrl: 'NO_IMAGE',
      releaseDate: '2024-08', totalTracks: 1, releaseType: 'SINGLE_EP', artists: [{ spotifyId: 'artist-1', name: 'Example Artist' }],
    });
    const getAllAlbumTracks = vi.spyOn(catalogDetailService, 'getAllAlbumTracks').mockResolvedValue({
      items: Array.from({ length: 12 }, (_, index) => ({ spotifyId: `track-${index + 1}`, title: index === 0 ? 'First Track' : `Track ${index + 1}`, artistName: 'Example Artist', discNumber: 1, trackNumber: index + 1, durationMs: 125000 })),
      totalCount: 12,
    });
    renderSubject('/album/album-1/example-album', 'ALBUM');

    expect(await screen.findByRole('heading', { name: 'Example Album' })).toBeInTheDocument();
    expect(await screen.findByText('Ambient')).toBeInTheDocument();
    expect(screen.getByText('Unverified tags')).toBeInTheDocument();
    expect(catalogSubjectTagsService.load).toHaveBeenCalledWith('ALBUM', 'album-1', expect.any(AbortSignal));
    expect(screen.getByText('Release type: Single / EP')).toBeInTheDocument();
    expect(await screen.findByText('First Track')).toBeInTheDocument();
    await waitFor(() => expect(document.querySelector('.catalog-subject-hero')).toHaveClass('catalog-subject-hero--ready'));
    expect(document.querySelector('.catalog-subject-track-number')).toHaveTextContent('1');
    expect(screen.getByText('2:05')).toBeInTheDocument();
    expect(getAllAlbumTracks).toHaveBeenCalledWith('album-1', expect.any(AbortSignal));
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
    expect(screen.getByText('Track 11')).toBeInTheDocument();
    expect(document.querySelectorAll('.catalog-subject-track-number')[10]).toHaveTextContent('11');
    expect(screen.queryByRole('navigation', { name: 'Tracks list pages' })).not.toBeInTheDocument();
  });

  it('keeps artist details visible while the index is building and retries the list', async () => {
    vi.spyOn(catalogDetailService, 'getArtist').mockResolvedValue({
      spotifyId: 'artist-1', name: 'Example Artist', imageUrl: 'NO_IMAGE', genres: [],
    });
    const getArtistAlbums = vi.spyOn(catalogDetailService, 'getArtistAlbums')
      .mockResolvedValueOnce({ status: 'BUILDING' })
      .mockResolvedValueOnce({
        items: [{ spotifyId: 'album-1', title: 'Ready Album', releaseType: 'ALBUM' }],
        totalCount: 1, classificationComplete: false, unclassifiedCount: 2,
      });

    renderSubject('/artist/artist-1/example-artist', 'ARTIST');

    expect(await screen.findByRole('heading', { name: 'Example Artist' })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Ready Album Album' }, { timeout: 3000 })).toBeInTheDocument();
    expect(getArtistAlbums).toHaveBeenCalledTimes(2);
  });

  it('shows a detail error while preserving a successful list', async () => {
    vi.spyOn(catalogDetailService, 'getArtist').mockRejectedValue(new Error('Failed'));
    vi.spyOn(catalogDetailService, 'getArtistAlbums').mockResolvedValue({
      items: [{ spotifyId: 'album-1', title: 'Still Here', imageUrl: 'NO_IMAGE', releaseDate: null }],
      totalCount: 1,
    });
    renderSubject('/artist/artist-1', 'ARTIST');

    expect(await screen.findByRole('alert')).toHaveTextContent('We could not load Artists information.');
    await waitFor(() => expect(screen.getByRole('button', { name: /Still Here/ })).toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Still Here' })).toBeInTheDocument();
  });

  it('omits the release type when the album detail cannot be classified', async () => {
    vi.spyOn(catalogDetailService, 'getAlbum').mockResolvedValue({
      spotifyId: 'album-1', title: 'Unclassified Album', imageUrl: 'NO_IMAGE', releaseType: null, artists: [],
    });
    vi.spyOn(catalogDetailService, 'getAllAlbumTracks').mockResolvedValue({ items: [], totalCount: 0 });
    renderSubject('/album/album-1/unclassified-album', 'ALBUM');

    expect(await screen.findByRole('heading', { name: 'Unclassified Album' })).toBeInTheDocument();
    expect(screen.queryByText(/Release type:/)).not.toBeInTheDocument();
  });

  it('does not import tags for an artist detail', async () => {
    vi.spyOn(catalogDetailService, 'getArtist').mockResolvedValue({ spotifyId: 'artist-1', name: 'Example Artist', genres: [] });
    vi.spyOn(catalogDetailService, 'getArtistAlbums').mockResolvedValue({ items: [], totalCount: 0 });
    renderSubject('/artist/artist-1/example-artist', 'ARTIST');

    expect(await screen.findByRole('heading', { name: 'Example Artist' })).toBeInTheDocument();
    expect(catalogSubjectTagsService.load).not.toHaveBeenCalled();
  });

  it('corrects a stale album slug after loading the ID', async () => {
    const getAlbum = vi.spyOn(catalogDetailService, 'getAlbum').mockResolvedValue({
      spotifyId: 'album-1', title: 'Correct Album', imageUrl: 'NO_IMAGE', artists: [],
    });
    vi.spyOn(catalogDetailService, 'getAllAlbumTracks').mockResolvedValue({ items: [], totalCount: 0 });
    renderSubject('/album/album-1/old-name', 'ALBUM');

    expect(await screen.findByText('Path: /album/album-1/correct-album')).toBeInTheDocument();
    expect(getAlbum).toHaveBeenCalledTimes(1);
  });
});
