import { describe, expect, it } from 'vitest';
import { formatRelativeTime } from './relative-time';

describe('formatRelativeTime', () => {
  const now = new Date('2026-11-05T12:00:00Z');
  const ago = (seconds: number) => new Date(now.getTime() - seconds * 1000).toISOString();

  it('reads under a minute as now', () => {
    expect(formatRelativeTime(ago(20), 'en', now)).toBe('now');
  });

  it('uses the largest whole unit', () => {
    expect(formatRelativeTime(ago(5 * 60), 'en', now)).toBe('5 minutes ago');
    expect(formatRelativeTime(ago(2 * 3600 + 120), 'en', now)).toBe('2 hours ago');
    expect(formatRelativeTime(ago(24 * 3600), 'en', now)).toBe('yesterday');
  });

  it('follows the locale', () => {
    expect(formatRelativeTime(ago(5 * 60), 'es', now)).toBe('hace 5 minutos');
  });

  it('clamps future timestamps to now', () => {
    expect(formatRelativeTime(ago(-90), 'en', now)).toBe('now');
  });
});
