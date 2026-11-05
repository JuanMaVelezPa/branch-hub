import { en } from './translations';
import { es } from './translations.es';

describe('translations', () => {
  it('has the same key set in en and es', () => {
    expect(Object.keys(es).sort()).toEqual(Object.keys(en).sort());
  });

  it('has no empty strings', () => {
    for (const dict of [en, es]) {
      for (const [key, value] of Object.entries(dict)) {
        expect(value.trim().length, key).toBeGreaterThan(0);
      }
    }
  });

  it('keeps the same {placeholders} in both locales', () => {
    const placeholders = (s: string) => (s.match(/\{[a-z]+\}/gi) ?? []).sort();
    for (const key of Object.keys(en) as (keyof typeof en)[]) {
      expect(placeholders(es[key]), key).toEqual(placeholders(en[key]));
    }
  });
});
