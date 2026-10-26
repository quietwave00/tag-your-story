export function getCookie(name) {
  if (typeof document === 'undefined') {
    return null;
  }

  const value =
    document.cookie
      .split('; ')
      .map((cookie) => {
        const separator = cookie.indexOf('=');
        return [cookie.slice(0, separator), cookie.slice(separator + 1)];
      })
      .find(([cookieName]) => cookieName === name)?.[1] ?? null;

  return value ? decodeURIComponent(value) : null;
}

export function deleteCookie(name, path = '/') {
  if (typeof document === 'undefined') {
    return;
  }

  document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=${path};`;
}
