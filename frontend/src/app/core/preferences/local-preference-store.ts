import { Injectable } from '@angular/core';
import type { LocaleCode, SidebarMode, ThemeMode } from './preference-keys';
import { PreferenceKeys } from './preference-keys';

/**
 * Thin wrapper around localStorage: namespaced keys, try/catch, SSR-safe.
 */
@Injectable({ providedIn: 'root' })
export class LocalPreferenceStore {
  getString(key: string): string | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  setString(key: string, value: string): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      localStorage.setItem(key, value);
    } catch {
      /* quota / private mode */
    }
  }

  remove(key: string): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      localStorage.removeItem(key);
    } catch {
      /* ignore */
    }
  }

  getTheme(): ThemeMode | null {
    const v = this.getString(PreferenceKeys.theme);
    return v === 'light' || v === 'dark' ? v : null;
  }

  setTheme(mode: ThemeMode): void {
    this.setString(PreferenceKeys.theme, mode);
  }

  getLocale(): LocaleCode | null {
    const v = this.getString(PreferenceKeys.locale);
    return v != null && v.length > 0 ? v : null;
  }

  setLocale(code: LocaleCode): void {
    this.setString(PreferenceKeys.locale, code);
  }

  /** Returns null for a missing or unknown value; callers apply the `expanded` default. */
  getSidebarMode(): SidebarMode | null {
    const v = this.getString(PreferenceKeys.sidebar);
    return v === 'expanded' || v === 'collapsed' ? v : null;
  }

  setSidebarMode(mode: SidebarMode): void {
    this.setString(PreferenceKeys.sidebar, mode);
  }
}
