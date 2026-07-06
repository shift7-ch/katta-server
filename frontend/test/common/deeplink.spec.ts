import { afterEach, describe, expect, it, vi } from 'vitest';

// Mock the config module to avoid its top-level HTTP request and to pin baseURL (incl. a non-root base path).
vi.mock('../../src/common/config', () => ({
  baseURL: '/foo/'
}));

import { openInKatta } from '../../src/common/deeplink';

describe('openInKatta', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('navigates to the katta:// deep link for the current host and base path', () => {
    vi.stubGlobal('location', { host: 'hub.example.com:8080', href: '' });

    openInKatta();

    expect(window.location.href).toBe('katta://hub.example.com:8080/foo/');
  });
});
