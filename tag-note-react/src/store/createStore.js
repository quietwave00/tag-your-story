import { useSyncExternalStore } from 'react';

export function createStore(initialState) {
  let state = initialState;
  const listeners = new Set();

  function getSnapshot() {
    return state;
  }

  function setState(updater) {
    const nextState = typeof updater === 'function' ? updater(state) : updater;
    state = { ...state, ...nextState };
    listeners.forEach((listener) => listener());
  }

  function subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  }

  function useStore(selector = (value) => value) {
    return useSyncExternalStore(subscribe, () => selector(getSnapshot()));
  }

  return {
    getState: getSnapshot,
    setState,
    subscribe,
    useStore,
  };
}
