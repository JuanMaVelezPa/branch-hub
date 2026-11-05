import { TestBed } from '@angular/core/testing';
import { UserPreferencesService } from '../preferences/user-preferences.service';
import { I18nService } from './i18n.service';

describe('I18nService', () => {
  let stored: string | null;
  let prefs: { getLocale: ReturnType<typeof vi.fn>; setLocale: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    stored = null;
    prefs = { getLocale: vi.fn(() => stored), setLocale: vi.fn() };
    TestBed.configureTestingModule({ providers: [{ provide: UserPreferencesService, useValue: prefs }] });
  });

  it('defaults to English when nothing is stored', async () => {
    const i18n = TestBed.inject(I18nService);
    await i18n.initialize();
    expect(i18n.locale()).toBe('en');
    expect(i18n.t('addBranch')).toBe('Add branch');
  });

  it('loads the stored Spanish dictionary before the first paint', async () => {
    stored = 'es';
    const i18n = TestBed.inject(I18nService);
    await i18n.initialize();
    expect(i18n.locale()).toBe('es');
    expect(i18n.t('addBranch')).toBe('Añadir sucursal');
    expect(document.documentElement.getAttribute('lang')).toBe('es');
  });

  it('switches only once the dictionary is loaded, then persists the choice', async () => {
    const i18n = TestBed.inject(I18nService);
    await i18n.initialize();
    const switching = i18n.setLocale('es');
    expect(i18n.locale()).toBe('en');
    await switching;
    expect(i18n.locale()).toBe('es');
    expect(prefs.setLocale).toHaveBeenLastCalledWith('es');
  });

  it('fills placeholders', async () => {
    const i18n = TestBed.inject(I18nService);
    await i18n.initialize();
    expect(i18n.t('pageSummary', { page: 1, pages: 3, total: 25 })).toBe('Page 1 of 3, 25 in total');
  });
});
