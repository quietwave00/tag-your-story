import { describe, expect, it } from 'vitest';
import { namespaces, resources } from './index.js';

function getLeafKeys(value, prefix = '') {
  return Object.entries(value).flatMap(([key, child]) => {
    const path = prefix ? `${prefix}.${key}` : key;

    if (child && typeof child === 'object' && !Array.isArray(child)) {
      return getLeafKeys(child, path);
    }

    return path;
  });
}

describe('translation resources', () => {
  it.each(namespaces)('has matching keys for the %s namespace', (namespace) => {
    expect(getLeafKeys(resources.ko[namespace]).sort()).toEqual(
      getLeafKeys(resources.en[namespace]).sort(),
    );
  });
});
