export function createDetailNavigationState(location) {
  return {
    ...location.state,
    returnTo: {
      path: `${location.pathname}${location.search}${location.hash ?? ''}`,
      state: location.state ?? null,
    },
  };
}

export function getDetailBackTarget(location) {
  if (location.state?.returnTo) return location.state.returnTo;
  if (location.state?.fromPage) return { path: location.state.fromPage, state: null };
  if (location.state?.fromSearch) return { path: location.state.fromSearch, state: null };
  return null;
}
