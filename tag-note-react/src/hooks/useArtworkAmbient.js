import { useEffect } from 'react';

function toHex(value) {
  return value.toString(16).padStart(2, '0');
}

export function pickArtworkColors(pixels) {
  const buckets = new Map();

  for (let index = 0; index < pixels.length; index += 4) {
    if (pixels[index + 3] < 160) continue;

    const red = Math.min(255, Math.floor(pixels[index] / 32) * 32 + 16);
    const green = Math.min(255, Math.floor(pixels[index + 1] / 32) * 32 + 16);
    const blue = Math.min(255, Math.floor(pixels[index + 2] / 32) * 32 + 16);
    const brightness = (red + green + blue) / 3;
    if (brightness < 35 || brightness > 225) continue;

    const key = `${red},${green},${blue}`;
    buckets.set(key, (buckets.get(key) ?? 0) + 1);
  }

  const candidates = [...buckets].map(([key, count]) => {
    const channels = key.split(',').map(Number);
    const saturation = Math.max(...channels) - Math.min(...channels);
    return { channels, score: count * (1 + saturation / 160) };
  }).sort((first, second) => second.score - first.score);

  const selected = [];
  for (const candidate of candidates) {
    const distinct = selected.every(({ channels }) => {
      const distance = channels.reduce((total, channel, index) => total + (channel - candidate.channels[index]) ** 2, 0);
      return distance > 65 ** 2;
    });
    if (distinct) selected.push(candidate);
    if (selected.length === 3) break;
  }

  return selected.map(({ channels }) => `#${channels.map(toHex).join('')}`);
}

export default function useArtworkAmbient(imageUrl) {
  useEffect(() => {
    if (!imageUrl || imageUrl === 'NO_IMAGE') return undefined;

    let disposed = false;
    document.body.style.setProperty('--artwork-ambient-image', `url("${encodeURI(imageUrl).replaceAll('"', '%22')}")`);
    document.body.classList.add('has-artwork-ambient', 'has-artwork-ambient-fallback');
    const image = new Image();
    image.crossOrigin = 'anonymous';
    image.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = 32;
        canvas.height = 32;
        const context = canvas.getContext('2d', { willReadFrequently: true });
        if (!context) return;
        context.drawImage(image, 0, 0, 32, 32);
        const colors = pickArtworkColors(context.getImageData(0, 0, 32, 32).data);
        if (disposed || colors.length === 0) return;

        document.body.style.setProperty('--artwork-ambient-one', colors[0]);
        document.body.style.setProperty('--artwork-ambient-two', colors[1] ?? colors[0]);
        document.body.style.setProperty('--artwork-ambient-three', colors[2] ?? colors[1] ?? colors[0]);
        document.body.classList.remove('has-artwork-ambient-fallback');
      } catch {
        // Remote artwork without canvas access uses a softly blurred copy of the image.
      }
    };
    image.src = imageUrl;

    return () => {
      disposed = true;
      image.onload = null;
      document.body.classList.remove('has-artwork-ambient', 'has-artwork-ambient-fallback');
      document.body.style.removeProperty('--artwork-ambient-image');
      document.body.style.removeProperty('--artwork-ambient-one');
      document.body.style.removeProperty('--artwork-ambient-two');
      document.body.style.removeProperty('--artwork-ambient-three');
    };
  }, [imageUrl]);
}
