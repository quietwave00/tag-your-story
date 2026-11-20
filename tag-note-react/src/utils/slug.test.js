import { describe, expect, it } from 'vitest';
import { toSlug } from './slug.js';

describe('toSlug', () => {
  it('turns titles and names into readable path segments', () => {
    expect(toSlug('A Night at the Opera')).toBe('a-night-at-the-opera');
    expect(toSlug('  꿈, 사랑 & 음악  ')).toBe('꿈-사랑-음악');
    expect(toSlug('AC/DC')).toBe('ac-dc');
  });

  it('keeps a usable segment when a name has no letters or numbers', () => {
    expect(toSlug('♪♪♪')).toBe('untitled');
  });
});
