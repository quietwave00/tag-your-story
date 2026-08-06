import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import NotificationCenter from '../../features/notifications/NotificationCenter.jsx';
import { authService } from '../../services/authService.js';
import { authStore, useAuthStore } from '../../store/authStore.js';
import { routes } from '../../utils/routes.js';

const navItems = [
  { to: routes.home, label: 'LOG' },
  { to: routes.tracks, label: 'DIG' },
  { to: routes.contact, label: 'CONTACT' },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  const handleLogout = async () => {
    try {
      await authService.logout();
    } finally {
      authStore.clear();
      navigate(routes.home);
    }
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <NavLink className="brand" to={routes.home}>
          #tagnote
        </NavLink>
        <nav className="app-nav" aria-label="Primary">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              to={item.to}
            >
              {item.label}
            </NavLink>
          ))}
          <NotificationCenter />
          {isAuthenticated ? (
            <button className="nav-action" onClick={handleLogout} type="button">
              EXIT
            </button>
          ) : (
            <NavLink
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              to={routes.login}
            >
              MY TAGS
            </NavLink>
          )}
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
