import { createBrowserRouter } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout.jsx';
import BoardDetailPage from '../pages/BoardDetailPage.jsx';
import BoardEditPage from '../pages/BoardEditPage.jsx';
import ContactPage from '../pages/ContactPage.jsx';
import ExceptionPage from '../pages/ExceptionPage.jsx';
import HomePage from '../pages/HomePage.jsx';
import LoginPage from '../pages/LoginPage.jsx';
import NicknamePage from '../pages/NicknamePage.jsx';
import TokenPage from '../pages/TokenPage.jsx';
import TrackDetailPage from '../pages/TrackDetailPage.jsx';
import TracksPage from '../pages/TracksPage.jsx';
import { routes } from '../utils/routes.js';

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    errorElement: <ExceptionPage />,
    children: [
      { path: routes.home, element: <HomePage /> },
      { path: routes.login, element: <LoginPage /> },
      { path: routes.token, element: <TokenPage /> },
      { path: routes.nickname, element: <NicknamePage /> },
      { path: routes.tracks, element: <TracksPage /> },
      { path: routes.contact, element: <ContactPage /> },
      { path: routes.trackDetail, element: <TrackDetailPage /> },
      { path: routes.boardCreate, element: <BoardEditPage /> },
      { path: routes.boardDetail, element: <BoardDetailPage /> },
      { path: routes.boardEdit, element: <BoardEditPage /> },
      { path: routes.exception, element: <ExceptionPage /> },
    ],
  },
]);
