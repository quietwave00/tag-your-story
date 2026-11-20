const NICKNAME_KEY = 'TagNoteNickname';

export const profileStorage = {
  getNickname() {
    return typeof localStorage === 'undefined' ? null : localStorage.getItem(NICKNAME_KEY);
  },

  setNickname(nickname) {
    if (typeof localStorage === 'undefined') return;
    if (nickname) localStorage.setItem(NICKNAME_KEY, nickname);
    else localStorage.removeItem(NICKNAME_KEY);
  },

  clear() {
    if (typeof localStorage !== 'undefined') localStorage.removeItem(NICKNAME_KEY);
  },
};
