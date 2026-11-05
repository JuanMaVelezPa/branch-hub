import type { Router } from '@angular/router';
import { isTranslationKey, type TranslationKey } from '../i18n/i18n.service';

/**
 * `data['titleKey']` of the deepest active route, or `fallback` when it is missing or not a known key.
 * Shared by the browser tab title (`App`) and the shell topbar.
 */
export function activeTitleKey(router: Router, fallback: TranslationKey = 'appTitle'): TranslationKey {
  let route = router.routerState.root;
  while (route.firstChild) {
    route = route.firstChild;
  }
  const raw = route.snapshot.data['titleKey'];
  return isTranslationKey(raw) ? raw : fallback;
}
