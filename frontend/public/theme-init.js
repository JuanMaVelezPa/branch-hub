/* Match key in src/app/core/preferences/preference-keys.ts (branch-hub.pref.v1.theme) — set data-theme before Angular boots. */
(function () {
  var k = 'branch-hub.pref.v1.theme';
  try {
    var v = localStorage.getItem(k);
    if (v === 'light' || v === 'dark') {
      document.documentElement.setAttribute('data-theme', v);
    } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
      document.documentElement.setAttribute('data-theme', 'dark');
    }
  } catch (e) {}
})();
