import { DestroyRef, inject, signal, type Signal } from '@angular/core';

/** Tailwind's `sm` breakpoint: the width from which list screens show a table instead of cards. */
export const SM_UP = '(min-width: 40rem)';

/**
 * Signal that tracks a CSS media query. Used to render either the table or the card list (not both),
 * so every `data-testid` and every row value exists once in the DOM at any width.
 * Without `matchMedia` (tests, SSR) it reports `true`.
 */
export function injectMediaQuery(query: string): Signal<boolean> {
  const mql = typeof matchMedia === 'function' ? matchMedia(query) : null;
  const matches = signal(mql?.matches ?? true);
  if (mql) {
    const onChange = (e: MediaQueryListEvent) => matches.set(e.matches);
    mql.addEventListener('change', onChange);
    inject(DestroyRef).onDestroy(() => mql.removeEventListener('change', onChange));
  }
  return matches.asReadonly();
}
