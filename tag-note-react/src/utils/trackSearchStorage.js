const SEARCH_KEYS = {
  keyword: 'keyword',
  page: 'page',
  trackId: 'trackId',
  title: 'title',
};

export function saveTrackSearch({ keyword, page = 1 }) {
  if (typeof sessionStorage === 'undefined') {
    return;
  }

  sessionStorage.setItem(SEARCH_KEYS.keyword, keyword);
  sessionStorage.setItem(SEARCH_KEYS.page, String(page));
}

export function getTrackSearch() {
  if (typeof sessionStorage === 'undefined') {
    return { keyword: null, page: 1 };
  }

  return {
    keyword: sessionStorage.getItem(SEARCH_KEYS.keyword),
    page: Number(sessionStorage.getItem(SEARCH_KEYS.page) ?? 1),
  };
}

export function saveSelectedTrack({ trackId, title }) {
  if (typeof sessionStorage === 'undefined') {
    return;
  }

  sessionStorage.setItem(SEARCH_KEYS.trackId, trackId);
  sessionStorage.setItem(SEARCH_KEYS.title, title);
}

export function getSelectedTrack() {
  if (typeof sessionStorage === 'undefined') {
    return { trackId: null, title: null };
  }

  return {
    trackId: sessionStorage.getItem(SEARCH_KEYS.trackId),
    title: sessionStorage.getItem(SEARCH_KEYS.title),
  };
}
