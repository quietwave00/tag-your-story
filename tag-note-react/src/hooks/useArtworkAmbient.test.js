import { describe, expect, it } from 'vitest';
import { pickArtworkColors } from './useArtworkAmbient.js';

describe('artwork ambient colors', () => {
  it('selects distinct representative colors and ignores transparent pixels', () => {
    const pixels = new Uint8ClampedArray([
      ...Array(16).fill([240, 96, 48]).flat(),
      ...Array(10).fill([48, 128, 224]).flat(),
      ...Array(6).fill([224, 192, 48]).flat(),
      16, 240, 16, 0,
    ]);

    expect(pickArtworkColors(pixels)).toEqual(['#f07030', '#3090f0', '#f0d030']);
  });

  it('returns no colors for an empty or fully transparent image', () => {
    expect(pickArtworkColors(new Uint8ClampedArray())).toEqual([]);
    expect(pickArtworkColors(new Uint8ClampedArray([220, 80, 40, 0]))).toEqual([]);
  });
});
