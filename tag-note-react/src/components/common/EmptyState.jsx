export default function EmptyState({ title, description }) {
  return (
    <section className="empty-state">
      <h2>{title}</h2>
      {description ? <p>{description}</p> : null}
    </section>
  );
}
