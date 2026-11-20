export async function loadGenreGraph(signal) {
  const response = await fetch(`${import.meta.env.BASE_URL}data/genre-graph.json`, { signal });
  if (!response.ok) throw new Error('Graph unavailable');
  return response.json();
}
