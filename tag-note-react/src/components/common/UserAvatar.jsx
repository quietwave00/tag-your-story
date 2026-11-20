export default function UserAvatar({ name, size = 'small' }) {
  const initial = Array.from(name ?? '')[0] ?? '';

  return (
    <span aria-hidden="true" className={`user-avatar user-avatar--${size}`}>
      {initial}
    </span>
  );
}
