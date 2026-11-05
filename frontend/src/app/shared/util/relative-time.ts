const UNITS: readonly [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
];

/**
 * Locale-aware relative time ("5 minutes ago", "hace 5 minutos", "yesterday") via `Intl.RelativeTimeFormat`.
 * Anything under a minute reads as "now". Future dates (clock skew) are clamped to "now".
 */
export function formatRelativeTime(value: string | Date, locale: string, now: Date = new Date()): string {
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' });
  const then = typeof value === 'string' ? new Date(value) : value;
  const seconds = Math.max(0, Math.round((now.getTime() - then.getTime()) / 1000));
  for (const [unit, size] of UNITS) {
    if (seconds >= size) {
      return rtf.format(-Math.floor(seconds / size), unit);
    }
  }
  return rtf.format(0, 'second');
}
