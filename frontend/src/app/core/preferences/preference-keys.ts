/**
 * Single namespace for persisted UI preferences. Bump version if stored shape changes.
 * Keep the theme key in sync with `public/theme-init.js` (FOUC prevention).
 */
export const PREFERENCE_STORAGE_VERSION = 'v1';

export const PREFERENCE_STORAGE_PREFIX = `branch-hub.pref.${PREFERENCE_STORAGE_VERSION}`;

export const PreferenceKeys = {
  theme: `${PREFERENCE_STORAGE_PREFIX}.theme`,
  /** BCP 47 tag, e.g. en, es — wire to i18n when added */
  locale: `${PREFERENCE_STORAGE_PREFIX}.locale`,
} as const;

export type ThemeMode = 'light' | 'dark';

export type LocaleCode = string;
