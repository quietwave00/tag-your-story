import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';

export default function NicknamePage() {
  const navigate = useNavigate();

  useEffect(() => {
    tokenStorage.discardLegacyPending();
    navigate(routes.login, { replace: true, state: { messageKey: 'oauth.legacyPending' } });
  }, [navigate]);

  return null;
}
