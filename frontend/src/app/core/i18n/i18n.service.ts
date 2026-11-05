import { Injectable, computed, signal } from '@angular/core';
import { UserPreferencesService } from '../preferences/user-preferences.service';
import { en, type Dictionary, type TranslationKey } from './translations';

export type AppLocale = 'en' | 'es';
export type { TranslationKey };

/** Route `data.titleKey` guard — avoids passing arbitrary strings into {@link I18nService#t}. */
export function isTranslationKey(value: unknown): value is TranslationKey {
  return typeof value === 'string' && Object.prototype.hasOwnProperty.call(en, value);
}

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly localeSignal = signal<AppLocale>('en');
  readonly locale = this.localeSignal.asReadonly();
  readonly isSpanish = computed(() => this.localeSignal() === 'es');

  /** `en` ships in the initial bundle; other locales are loaded on first use. */
  private readonly dictionaries: Partial<Record<AppLocale, Dictionary>> = { en };

  constructor(private readonly prefs: UserPreferencesService) {}

  /** Run from the app initializer: waits for a stored non-English dictionary so the first paint is translated. */
  initialize(): Promise<void> {
    const stored = this.prefs.getLocale();
    // Product decision: default locale is always English unless user explicitly changes it.
    return this.setLocale(stored === 'es' ? 'es' : 'en');
  }

  /** Switches only after the dictionary is loaded, so the UI never mixes languages. */
  async setLocale(locale: AppLocale): Promise<void> {
    await this.loadDictionary(locale);
    this.localeSignal.set(locale);
    this.prefs.setLocale(locale);
    document.documentElement.setAttribute('lang', locale);
  }

  private async loadDictionary(locale: AppLocale): Promise<void> {
    if (!this.dictionaries[locale]) {
      this.dictionaries[locale] = (await import('./translations.es')).es;
    }
  }

  t(key: TranslationKey, vars?: Record<string, string | number>): string {
    let text: string = (this.dictionaries[this.localeSignal()] ?? en)[key];
    if (!vars) {
      return text;
    }
    // Longer keys first so `{page}` never partial-matches inside `{pages}`; replaceAll for every occurrence.
    const entries = Object.entries(vars).sort((a, b) => b[0].length - a[0].length);
    for (const [name, value] of entries) {
      text = text.replaceAll(`{${name}}`, String(value));
    }
    return text;
  }
}
