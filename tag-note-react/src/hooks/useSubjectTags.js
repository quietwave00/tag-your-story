import { useEffect, useState } from 'react';
import { catalogSubjectTagsService } from '../services/catalogSubjectTagsService.js';

export default function useSubjectTags(subjectType, spotifyId) {
  const key = `${subjectType}:${spotifyId}`;
  const [state, setState] = useState({ key: null, tags: null, loading: true, error: false });

  useEffect(() => {
    if (!spotifyId) return undefined;
    const controller = new AbortController();
    let active = true;
    setState({ key, tags: null, loading: true, error: false });

    catalogSubjectTagsService.load(subjectType, spotifyId, controller.signal)
      .then((tags) => { if (active) setState({ key, tags, loading: false, error: false }); })
      .catch(() => { if (active) setState({ key, tags: null, loading: false, error: true }); });

    return () => { active = false; controller.abort(); };
  }, [key, spotifyId, subjectType]);

  return state.key === key ? state : { tags: null, loading: true, error: false };
}
