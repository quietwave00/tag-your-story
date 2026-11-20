import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes, useLocation, useParams } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { catalogSearchService } from '../services/catalogSearchService.js';
import { toSlug } from '../utils/slug.js';
import TracksPage from './TracksPage.jsx';

const artist = { subjectType: 'ARTIST', spotifyId: 'artist-spotify', title: 'Queen', imageUrl: 'https://example.com/artist.jpg' };
const track = { subjectType: 'TRACK', spotifyId: 'track-spotify', title: 'Song', artistName: 'Queen', albumName: 'Album', imageUrl: 'https://example.com/track.jpg' };
const album = { subjectType: 'ALBUM', spotifyId: 'album-spotify', title: 'A Night at the Opera', artistName: 'Queen', imageUrl: 'https://example.com/album.jpg' };

function response({ artists = [artist], tracks = [track], albums = [album], artistHasNext = false, trackHasNext = false, albumHasNext = false, totalCount = 1 } = {}) {
  return {
    artists: { items: artists, totalCount, hasNext: artistHasNext },
    tracks: { items: tracks, totalCount, hasNext: trackHasNext },
    albums: { items: albums, totalCount, hasNext: albumHasNext },
  };
}

function deferred() {
  let resolve;
  const promise = new Promise((finish) => { resolve = finish; });
  return { promise, resolve };
}

function Destination({ parameter }) {
  const params = useParams();
  return <p>Destination: {params[parameter]} / {params.slug}</p>;
}

function CurrentLocation() {
  const location = useLocation();
  return <p data-testid="search-location">{location.search}</p>;
}

function renderSearch(path = '/search?keyword=Queen&page=1') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <CurrentLocation />
      <Routes>
        <Route path="/search" element={<TracksPage />} />
        <Route path="/track/:trackId/:slug" element={<Destination parameter="trackId" />} />
        <Route path="/artist/:spotifyArtistId/:slug" element={<Destination parameter="spotifyArtistId" />} />
        <Route path="/album/:spotifyAlbumId/:slug" element={<Destination parameter="spotifyAlbumId" />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  sessionStorage.clear();
  await i18n.changeLanguage('en');
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
});

afterEach(() => { cleanup(); vi.restoreAllMocks(); delete Element.prototype.scrollIntoView; });

describe('screen catalog search', () => {
  it('makes no request without a keyword and keeps three sections', () => {
    const search = vi.spyOn(catalogSearchService, 'searchScreen');
    renderSearch('/search');
    expect(screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent))
      .toEqual(['Artists', 'Tracks', 'Albums']);
    expect(search).not.toHaveBeenCalled();
  });

  it('shows one wave loader, then fills all sections from one request', async () => {
    const pending = deferred();
    const search = vi.spyOn(catalogSearchService, 'searchScreen').mockReturnValue(pending.promise);
    renderSearch();
    expect(screen.getByRole('status')).toHaveTextContent('Loading search results');
    expect(screen.getByRole('status').querySelectorAll('.catalog-loading-artist')).toHaveLength(5);
    expect(screen.queryByRole('region', { name: 'Artists' })).not.toBeInTheDocument();
    expect(search).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenCalledWith(expect.objectContaining({ keyword: 'Queen', trackPage: 1, albumPage: 1, artistPage: 1 }));

    await act(async () => { pending.resolve(response()); await pending.promise; });
    expect(within(screen.getByRole('region', { name: 'Artists' })).getByText('Queen')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Tracks' })).getByText('Song')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Albums' })).getByText('A Night at the Opera')).toBeInTheDocument();
    expect(search).toHaveBeenCalledTimes(1);
  });

  it('focuses and scrolls the changed result section after pagination', async () => {
    vi.spyOn(catalogSearchService, 'searchScreen').mockResolvedValue(response({ trackHasNext: true, albumHasNext: true }));
    const scrollIntoView = vi.fn();
    Element.prototype.scrollIntoView = scrollIntoView;
    renderSearch();
    const albums = await screen.findByRole('region', { name: 'Albums' });
    fireEvent.click(within(albums).getByRole('button', { name: 'Next page' }));
    const heading = within(albums).getByRole('heading', { name: 'Albums' });
    expect(heading).toHaveFocus();
    expect(scrollIntoView).toHaveBeenCalledWith(expect.objectContaining({ behavior: 'smooth', block: 'start' }));
  });

  it('keeps artist card groups and loads a new server page only after the last group', async () => {
    const artistItems = Array.from({ length: 7 }, (_, index) => ({ ...artist, spotifyId: `artist-${index}`, title: `Artist ${index}` }));
    const nextArtists = deferred();
    const search = vi.spyOn(catalogSearchService, 'searchScreen').mockImplementation(({ artistPage }) =>
      artistPage === 2 ? nextArtists.promise : Promise.resolve(response({ artists: artistItems, artistHasNext: true })));
    renderSearch();
    const artists = await screen.findByRole('region', { name: 'Artists' });
    expect(await within(artists).findByText('Artist 0')).toBeInTheDocument();
    expect(within(artists).getByRole('button', { name: 'Previous page' })).toBeDisabled();
    const firstGroup = artists.querySelector('.catalog-artist-list');

    fireEvent.click(within(artists).getByRole('button', { name: 'Next page' }));
    expect(artists.querySelector('.catalog-artist-list')).not.toBe(firstGroup);
    expect(artists.querySelector('.catalog-artist-list')).toHaveAttribute('data-direction', 'next');
    expect(within(artists).getByText('Artist 5')).toBeInTheDocument();
    expect(search).toHaveBeenCalledTimes(1);

    fireEvent.click(within(artists).getByRole('button', { name: 'Next page' }));
    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ artistPage: 2, trackPage: 1, albumPage: 1 }));
    expect(artists.querySelector('.catalog-artist-stage')).toHaveClass('catalog-artist-stage--loading');
    expect(within(screen.getByRole('region', { name: 'Tracks' })).getByText('Song')).toBeInTheDocument();
    expect(screen.queryByText('Loading search results')).not.toBeInTheDocument();

    await act(async () => { nextArtists.resolve(response({ artists: [{ ...artist, spotifyId: 'new-artist', title: 'Next Artist' }], artistHasNext: false })); await nextArtists.promise; });
    expect(await within(artists).findByText('Next Artist')).toBeInTheDocument();
    expect(artists.querySelector('.catalog-artist-list')).toHaveAttribute('data-direction', 'next');
    expect(within(artists).getByRole('button', { name: 'Next page' })).toBeDisabled();
    fireEvent.click(within(artists).getByRole('button', { name: 'Previous page' }));
    await waitFor(() => expect(search).toHaveBeenCalledTimes(3));
    expect(await within(artists).findByText('Artist 5')).toBeInTheDocument();
    expect(artists.querySelector('.catalog-artist-list')).toHaveAttribute('data-direction', 'previous');
  });

  it('uses hasNext for track and album paging without numbered totalCount pages', async () => {
    const nextTracks = deferred();
    const search = vi.spyOn(catalogSearchService, 'searchScreen').mockImplementation(({ trackPage, albumPage }) => {
      if (trackPage === 2) return nextTracks.promise;
      return Promise.resolve(response({ trackHasNext: true, albumHasNext: albumPage === 1, totalCount: 5000 }));
    });
    renderSearch();
    const tracks = await screen.findByRole('region', { name: 'Tracks' });
    const albums = screen.getByRole('region', { name: 'Albums' });
    expect(await within(tracks).findByText('Song')).toBeInTheDocument();
    expect(within(tracks).queryByRole('button', { name: '500' })).not.toBeInTheDocument();
    expect(within(tracks).getByRole('button', { name: 'Previous page' })).toBeDisabled();
    expect(within(tracks).getByRole('button', { name: 'Next page' })).toBeEnabled();

    fireEvent.click(within(tracks).getByRole('button', { name: 'Next page' }));
    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    expect(within(tracks).getByRole('status')).toHaveTextContent('Loading Tracks results');
    expect(within(albums).getByText('A Night at the Opera')).toBeInTheDocument();
    await act(async () => { nextTracks.resolve(response({ tracks: [{ ...track, spotifyId: 'next-track', title: 'Second Song' }], trackHasNext: false, albumHasNext: true, totalCount: 5000 })); await nextTracks.promise; });
    expect(await within(tracks).findByText('Second Song')).toBeInTheDocument();
    expect(within(tracks).getByRole('button', { name: 'Next page' })).toBeDisabled();
    expect(screen.getByTestId('search-location')).toHaveTextContent('page=2');
    expect(screen.getByTestId('search-location')).not.toHaveTextContent('albumPage=2');

    fireEvent.click(within(albums).getByRole('button', { name: 'Next page' }));
    await waitFor(() => expect(search).toHaveBeenCalledTimes(3));
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ trackPage: 2, albumPage: 2, artistPage: 1 }));
  });

  it('keeps the previous page and closes Next when a speculative next page is empty', async () => {
    const search = vi.spyOn(catalogSearchService, 'searchScreen').mockImplementation(({ trackPage }) =>
      Promise.resolve(trackPage === 1 ? response({ trackHasNext: true, totalCount: 200 }) : response({ tracks: [], trackHasNext: false, totalCount: 200 })));
    renderSearch();
    const tracks = await screen.findByRole('region', { name: 'Tracks' });
    expect(await within(tracks).findByText('Song')).toBeInTheDocument();

    fireEvent.click(within(tracks).getByRole('button', { name: 'Next page' }));
    await waitFor(() => expect(within(tracks).getByRole('button', { name: 'Next page' })).toBeDisabled());
    expect(within(tracks).getByText('Song')).toBeInTheDocument();
    expect(within(tracks).getByRole('button', { name: 'Previous page' })).toBeDisabled();
    expect(screen.getByTestId('search-location')).toHaveTextContent('page=1');
    expect(search).toHaveBeenCalledTimes(2);
  });

  it('keeps the last artist card group when the next server page is empty', async () => {
    const artistItems = Array.from({ length: 7 }, (_, index) => ({ ...artist, spotifyId: `artist-${index}`, title: `Artist ${index}` }));
    const search = vi.spyOn(catalogSearchService, 'searchScreen').mockImplementation(({ artistPage }) =>
      Promise.resolve(artistPage === 1
        ? response({ artists: artistItems, artistHasNext: true })
        : response({ artists: [], artistHasNext: false })));
    renderSearch();
    const artists = await screen.findByRole('region', { name: 'Artists' });
    expect(await within(artists).findByText('Artist 0')).toBeInTheDocument();
    fireEvent.click(within(artists).getByRole('button', { name: 'Next page' }));
    expect(within(artists).getByText('Artist 5')).toBeInTheDocument();
    fireEvent.click(within(artists).getByRole('button', { name: 'Next page' }));

    await waitFor(() => expect(within(artists).getByRole('button', { name: 'Next page' })).toBeDisabled());
    expect(within(artists).getByText('Artist 5')).toBeInTheDocument();
    expect(screen.getByTestId('search-location')).toHaveTextContent('artistPage=1');
    expect(search).toHaveBeenCalledTimes(2);
  });

  it('rolls back only an empty section when two page changes overlap', async () => {
    const firstNext = deferred();
    const search = vi.spyOn(catalogSearchService, 'searchScreen').mockImplementation(({ trackPage, albumPage }) => {
      if (trackPage === 2 && albumPage === 1) return firstNext.promise;
      if (trackPage === 2 && albumPage === 2) return Promise.resolve(response({
        tracks: [], albums: [{ ...album, title: 'Next Album' }], trackHasNext: false, albumHasNext: false,
      }));
      return Promise.resolve(response({ trackHasNext: true, albumHasNext: true }));
    });
    renderSearch();
    const tracks = await screen.findByRole('region', { name: 'Tracks' });
    const albums = screen.getByRole('region', { name: 'Albums' });
    expect(await within(tracks).findByText('Song')).toBeInTheDocument();

    fireEvent.click(within(tracks).getByRole('button', { name: 'Next page' }));
    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    fireEvent.click(within(albums).getByRole('button', { name: 'Next page' }));
    expect(await within(albums).findByText('Next Album')).toBeInTheDocument();
    expect(within(tracks).getByText('Song')).toBeInTheDocument();
    expect(within(tracks).getByRole('button', { name: 'Next page' })).toBeDisabled();
    expect(screen.getByTestId('search-location')).toHaveTextContent('page=1');
    expect(screen.getByTestId('search-location')).toHaveTextContent('albumPage=2');
    expect(search.mock.calls[1][0].signal.aborted).toBe(true);
    await act(async () => { firstNext.resolve(response()); await firstNext.promise; });
    expect(within(albums).getByText('Next Album')).toBeInTheDocument();
  });

  it('shows an error in each section if the screen request fails', async () => {
    vi.spyOn(catalogSearchService, 'searchScreen').mockRejectedValue(new Error('Spotify unavailable'));
    renderSearch();
    expect(await within(await screen.findByRole('region', { name: 'Artists' })).findByText('We could not load Artists.')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Tracks' })).getByText('We could not load Tracks.')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Albums' })).getByText('We could not load Albums.')).toBeInTheDocument();
  });

  it.each([
    ['ARTIST', 'Artists', 'Queen', 'artist-spotify'],
    ['TRACK', 'Tracks', 'Song', 'track-spotify'],
    ['ALBUM', 'Albums', 'A Night at the Opera', 'album-spotify'],
  ])('opens the %s result with an ID and name slug', async (type, label, title, spotifyId) => {
    vi.spyOn(catalogSearchService, 'searchScreen').mockResolvedValue(response());
    renderSearch();
    const section = await screen.findByRole('region', { name: label });
    fireEvent.click(await within(section).findByRole('button', { name: new RegExp(title) }));
    expect(await screen.findByText(`Destination: ${spotifyId} / ${toSlug(title)}`)).toBeInTheDocument();
    if (type === 'TRACK') expect(sessionStorage.getItem('trackId')).toBe('track-spotify');
  });

  it('aborts an old keyword request and resets all three pages', async () => {
    const oldRequest = deferred();
    const search = vi.spyOn(catalogSearchService, 'searchScreen').mockImplementation(({ keyword }) =>
      keyword === 'Q' ? oldRequest.promise : Promise.resolve(response()));
    renderSearch('/search?keyword=Q&page=3&artistPage=2&albumPage=4');
    fireEvent.change(screen.getByRole('searchbox', { name: 'Search the catalog' }), { target: { value: 'Queen' } });
    expect(await within(await screen.findByRole('region', { name: 'Tracks' })).findByText('Song')).toBeInTheDocument();
    expect(search).toHaveBeenCalledTimes(2);
    expect(search).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: 'Queen', trackPage: 1, albumPage: 1, artistPage: 1 }));
    expect(search.mock.calls[0][0].signal.aborted).toBe(true);
    await act(async () => { oldRequest.resolve(response({ tracks: [{ ...track, title: 'Stale' }] })); await oldRequest.promise; });
    expect(screen.queryByText('Stale')).not.toBeInTheDocument();
    expect(screen.getByTestId('search-location')).toHaveTextContent('albumPage=1');
  });
});
