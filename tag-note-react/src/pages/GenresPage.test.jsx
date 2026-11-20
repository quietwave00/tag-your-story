import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom';
import i18n from '../i18n/index.js';
import GenresPage from './GenresPage.jsx';
import { normalizeGraph } from '../features/genreGraph/model.js';
import { useGenreGraph } from '../features/genreGraph/useGenreGraph.js';
vi.mock('../features/genreGraph/useGenreGraph.js', () => ({ useGenreGraph: vi.fn() }));
vi.mock('../features/genreGraph/GenreCanvas.jsx', () => ({ default: () => <div data-testid="canvas" /> }));
const raw = { schemaVersion: 1, graphVersion: 'v1', completeness: 'COMPLETE', nodes: [{ id: 'a', name: 'Rock' }, { id: 'b', name: 'Jazz' }, { id: 'c', name: 'Isolated' }], edges: [{ source: 'a', target: 'b', type: 'INFLUENCES' }] };
function Navigation() {
  const location = useLocation(), navigate = useNavigate();
  return <><output data-testid="url">{location.search}</output><button onClick={() => navigate(-1)}>Back</button></>;
}
function mount(url = '/genres') { return render(<MemoryRouter initialEntries={[url]}><GenresPage /><Navigation /></MemoryRouter>); }
beforeEach(async () => { await i18n.changeLanguage('en'); useGenreGraph.mockReturnValue({ status: 'ready', graph: normalizeGraph(raw), positions: [] }); });
afterEach(() => { cleanup(); vi.clearAllMocks(); });
describe('genre exploration', () => {
  it('restores a direct selection, preserves other parameters and filters only relationships', async () => {
    mount('/genres?node=a&other=keep');
    expect(screen.getByTestId('url')).toHaveTextContent('node=a');
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: /Influence/ }));
    expect(screen.getByRole('checkbox', { name: /Influence/ })).not.toBeChecked();
    expect(screen.getByTestId('url')).toHaveTextContent('node=a');
    fireEvent.click(screen.getByRole('button', { name: 'Full view' }));
    expect(screen.getByTestId('url')).toHaveTextContent('?other=keep');
    expect(screen.getByRole('checkbox', { name: /Influence/ })).toBeChecked();
    await screen.findByTestId('canvas');
  });
  it('supports keyboard search and browser history without opening an aside', async () => {
    mount();
    const input = screen.getByRole('combobox');
    fireEvent.change(input, { target: { value: 'rock' } });
    fireEvent.keyDown(input, { key: 'ArrowDown' }); fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('url')).toHaveTextContent('node=a');
    fireEvent.change(input, { target: { value: 'jazz' } });
    fireEvent.keyDown(input, { key: 'ArrowDown' }); fireEvent.keyDown(input, { key: 'Enter' });
    expect(screen.getByTestId('url')).toHaveTextContent('node=b');
    fireEvent.click(screen.getByRole('button', { name: 'Back' }));
    await waitFor(() => expect(screen.getByTestId('url')).toHaveTextContent('node=a'));
    fireEvent.click(screen.getByRole('button', { name: 'Full view' }));
    expect(input).toHaveFocus();
  });
  it('removes only an invalid node parameter and announces it', async () => {
    mount('/genres?node=missing&other=keep');
    await waitFor(() => expect(screen.getByTestId('url')).toHaveTextContent('?other=keep'));
    expect(screen.getByText(/not in the current snapshot/)).toBeInTheDocument();
  });
  it('preserves isolated genres and warns about partial data', () => {
    useGenreGraph.mockReturnValue({ status: 'ready', graph: normalizeGraph({ ...raw, completeness: 'PARTIAL' }), positions: [] });
    mount('/genres?node=c');
    expect(screen.getByText(/partial collection/)).toBeInTheDocument();
    expect(screen.getByTestId('url')).toHaveTextContent('node=c');
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument();
  });
  it('distinguishes a failed load from empty data and offers retry', () => {
    const retry = vi.fn(); useGenreGraph.mockReturnValue({ status: 'error', retry }); mount();
    expect(screen.getByRole('alert')).toHaveTextContent('Could not load');
    fireEvent.click(screen.getByRole('button', { name: 'Try again' })); expect(retry).toHaveBeenCalledOnce();
    expect(screen.queryByText('This snapshot has no genres.')).not.toBeInTheDocument();
  });
});
