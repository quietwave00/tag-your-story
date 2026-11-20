export function formatCreatedAt(createdAt, language) {
  if (!createdAt) return '';

  if (Array.isArray(createdAt)) {
    const [year, month, day] = createdAt;
    return new Intl.DateTimeFormat(language).format(new Date(year, month - 1, day));
  }

  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) return String(createdAt);
  return new Intl.DateTimeFormat(language).format(date);
}
