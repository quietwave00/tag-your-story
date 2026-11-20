import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate, useLocation, useParams } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout.jsx';
import BoardDetailPage from '../pages/BoardDetailPage.jsx';
import BoardEditPage from '../pages/BoardEditPage.jsx';
import CatalogSubjectPage from '../pages/CatalogSubjectPage.jsx';
import ContactPage from '../pages/ContactPage.jsx';
import ExceptionPage from '../pages/ExceptionPage.jsx';
import HomePage from '../pages/HomePage.jsx';
import LoginPage from '../pages/LoginPage.jsx';
import MyPage from '../pages/MyPage.jsx';
import SignupPage from '../pages/SignupPage.jsx';
import NicknamePage from '../pages/NicknamePage.jsx';
import TokenPage from '../pages/TokenPage.jsx';
import TrackDetailPage from '../pages/TrackDetailPage.jsx';
import TracksPage from '../pages/TracksPage.jsx';
import { routes } from '../utils/routes.js';

const GenresPage = lazy(() => import('../pages/GenresPage.jsx'));

function LegacyDetailRedirect({ type }) {
  const params = useParams();
  const location = useLocation();
  const id = type === 'track' ? params.trackId : type === 'artist' ? params.spotifyArtistId : params.spotifyAlbumId;
  return <Navigate replace state={location.state} to={`/${type}/${encodeURIComponent(id)}${params.slug ? `/${encodeURIComponent(params.slug)}` : ''}${location.search}${location.hash}`} />;
}

function LegacySearchRedirect() {
  const location = useLocation();
  return <Navigate replace state={location.state} to={`${routes.tracks}${location.search}${location.hash}`} />;
}

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    errorElement: <ExceptionPage />,
    children: [
      { path: routes.home, element: <HomePage /> },
      { path: routes.genres, element: <Suspense fallback={null}><GenresPage /></Suspense> },
      { path: routes.login, element: <LoginPage /> },
      { path: routes.signup, element: <SignupPage /> },
      { path: routes.myPage, element: <MyPage /> },
      { path: routes.token, element: <TokenPage /> },
      { path: routes.nickname, element: <NicknamePage /> },
      { path: routes.tracks, element: <TracksPage /> },
      { path: routes.tracksLegacy, element: <LegacySearchRedirect /> },
      { path: routes.contact, element: <ContactPage /> },
      { path: routes.trackDetail, element: <TrackDetailPage /> },
      { path: routes.trackDetailLegacy, element: <TrackDetailPage /> },
      { path: routes.artistDetail, element: <CatalogSubjectPage type="ARTIST" /> },
      { path: routes.artistDetailLegacy, element: <CatalogSubjectPage type="ARTIST" /> },
      { path: routes.albumDetail, element: <CatalogSubjectPage type="ALBUM" /> },
      { path: routes.albumDetailLegacy, element: <CatalogSubjectPage type="ALBUM" /> },
      { path: '/tracks/:trackId/:slug', element: <LegacyDetailRedirect type="track" /> },
      { path: '/tracks/:trackId', element: <LegacyDetailRedirect type="track" /> },
      { path: '/artists/:spotifyArtistId/:slug', element: <LegacyDetailRedirect type="artist" /> },
      { path: '/artists/:spotifyArtistId', element: <LegacyDetailRedirect type="artist" /> },
      { path: '/albums/:spotifyAlbumId/:slug', element: <LegacyDetailRedirect type="album" /> },
      { path: '/albums/:spotifyAlbumId', element: <LegacyDetailRedirect type="album" /> },
      { path: routes.boardCreate, element: <BoardEditPage /> },
      { path: routes.boardDetail, element: <BoardDetailPage /> },
      { path: routes.boardEdit, element: <BoardEditPage /> },
      { path: routes.exception, element: <ExceptionPage /> },
    ],
  },
]);
