import { afterEach, describe, expect, it, vi } from 'vitest';

import { detectOS } from '../../src/common/os';

describe('detectOS', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('detects macOS from a desktop Safari userAgent', () => {
    vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15' });

    expect(detectOS()).toBe('mac');
  });

  it('detects Windows from a desktop Chrome userAgent', () => {
    vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36' });

    expect(detectOS()).toBe('win');
  });

  it('falls back to other for Linux', () => {
    vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0' });

    expect(detectOS()).toBe('other');
  });

  it('falls back to other for Android', () => {
    vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36' });

    expect(detectOS()).toBe('other');
  });

  it('falls back to other for iPadOS desktop mode reporting Macintosh with touch support', () => {
    vi.stubGlobal('navigator', { userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15', maxTouchPoints: 5 });

    expect(detectOS()).toBe('other');
  });

  it('falls back to other when navigator is unavailable (non-browser runtime)', () => {
    vi.stubGlobal('navigator', undefined);

    expect(detectOS()).toBe('other');
  });
});
