import { expect, test, type Page } from '@playwright/test';
import path from 'node:path';

/**
 * Captures every screen at phone and desktop width, in light and dark, for the manual design
 * review (SPEC 03). No pixel-diff assertions: font rendering differs across hosts.
 *
 * Output: e2e/__screenshots__/<screen>-<width>-<theme>.png (gitignored).
 * Run against Compose: `npm run test:e2e:screenshots`. `DOCS_IMAGES=1` also refreshes docs/images (see the end of the file).
 */

const OUT_DIR = path.resolve('e2e/__screenshots__');
const WIDTHS = [390, 1280] as const;
const THEMES = ['light', 'dark'] as const;
const SHOT_BRANCH_CODE = 'SHOT-01';

type Theme = (typeof THEMES)[number];

/** The access token lives in memory, so a reload signs out: move between screens inside the SPA. */
async function navigate(page: Page, url: string): Promise<void> {
  await page.evaluate((target) => {
    history.pushState(null, '', target);
    dispatchEvent(new PopStateEvent('popstate', { state: null }));
  }, url);
  await expect(page).toHaveURL(new RegExp(`${url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`));
}

/** Waits for data, fonts and toasts to settle so each image shows the finished screen. */
async function settle(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle');
  await page.evaluate(() => document.fonts.ready);
  await expect(page.locator('.toast')).toHaveCount(0, { timeout: 6_000 });
  await page.mouse.move(0, 0);
}

for (const width of WIDTHS) {
  for (const theme of THEMES) {
    test.describe(`screens at ${width}px, ${theme}`, () => {
      test.use({ viewport: { width, height: width < 640 ? 844 : 800 }, colorScheme: theme });

      test(`capture ${width} ${theme}`, async ({ page }) => {
        test.setTimeout(120_000);
        await page.addInitScript((t: Theme) => {
          try {
            localStorage.setItem('branch-hub.pref.v1.theme', t);
            localStorage.setItem('branch-hub.pref.v1.locale', 'en');
          } catch {
            /* storage blocked: colorScheme still selects the theme */
          }
        }, theme);

        // A modal's backdrop only covers the viewport, so capture modals without fullPage.
        const shot = async (screen: string, fullPage = true) => {
          await settle(page);
          await page.screenshot({
            path: path.join(OUT_DIR, `${screen}-${width}-${theme}.png`),
            fullPage,
            animations: 'disabled',
          });
        };

        await page.goto('/login');
        await expect(page.getByTestId('login-submit')).toBeVisible();
        await shot('login');

        await page.getByTestId('login-username').fill(process.env.E2E_USER ?? 'admin@example.com');
        await page.getByTestId('login-password').fill(process.env.E2E_PASSWORD ?? 'Admin_ChangeMe_2026!');
        await page.getByTestId('login-submit').click();
        await expect(page).toHaveURL(/\/dashboard$/);
        await expect(page.getByTestId('dashboard-summary')).toBeVisible();
        await shot('dashboard');

        await navigate(page, '/branches');
        await expect(page.getByTestId('branch-add')).toBeVisible();
        const branchLinks = page.locator('main a[href^="/branches/"]:not([href="/branches/new"])');
        await settle(page);
        if ((await branchLinks.count()) === 0) {
          // Empty registry: add one branch so detail and edit have something to show.
          await navigate(page, '/branches/new');
          await page.getByTestId('branch-code').fill(SHOT_BRANCH_CODE);
          await page.getByTestId('branch-name').fill('Screenshot branch');
          await page.getByTestId('branch-city').fill('Bogota');
          await page.getByTestId('branch-submit').click();
          await expect(page).toHaveURL(/\/branches$/);
          await expect(branchLinks.first()).toBeVisible();
        }
        await shot('branches-list');

        const detailHref = await branchLinks.first().getAttribute('href');
        expect(detailHref).toBeTruthy();
        await navigate(page, detailHref!);
        await expect(page.getByTestId('branch-detail-code')).toBeVisible();
        await shot('branch-detail');

        await navigate(page, `${detailHref}/edit`);
        await expect(page.getByTestId('branch-edit-submit')).toBeVisible();
        await shot('branch-edit');

        await navigate(page, '/branches/new');
        await expect(page.getByTestId('branch-submit')).toBeVisible();
        await shot('branch-create');

        await navigate(page, '/admin/users');
        await expect(page.getByTestId('admin-create-user-open')).toBeVisible();
        await shot('admin-users');

        await page.getByTestId('admin-create-user-open').click();
        await expect(page.getByTestId('admin-create-user-modal')).toBeVisible();
        await shot('admin-create-user-modal', false);
        await page.keyboard.press('Escape');
        await expect(page.getByTestId('admin-create-user-modal')).toBeHidden();

        await navigate(page, '/admin/roles');
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await shot('admin-roles');

        await navigate(page, '/admin/audit-log');
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await shot('admin-audit-log');

        await navigate(page, '/me');
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
        await shot('profile');
      });
    });
  }
}

/**
 * Opt-in: refreshes the images the root README embeds (docs/images/, committed) at 1440x900, light:
 * login, dashboard, branches and admin-users. grafana.png is captured by hand.
 * Run with `DOCS_IMAGES=1 npm run test:e2e:screenshots`.
 */
test.describe('README images', () => {
  test.skip(process.env['DOCS_IMAGES'] !== '1', 'set DOCS_IMAGES=1 to refresh docs/images');
  test.use({ viewport: { width: 1440, height: 900 }, colorScheme: 'light' });

  test('capture docs/images', async ({ page }) => {
    const docsDir = path.resolve('../docs/images');
    await page.addInitScript(() => {
      try {
        localStorage.setItem('branch-hub.pref.v1.theme', 'light');
        localStorage.setItem('branch-hub.pref.v1.locale', 'en');
        localStorage.setItem('branch-hub.pref.v1.sidebar', 'expanded');
      } catch {
        /* storage blocked: defaults are light, en, expanded */
      }
    });
    const shot = async (name: string) => {
      await settle(page);
      await page.screenshot({ path: path.join(docsDir, `${name}.png`), animations: 'disabled' });
    };

    await page.goto('/login');
    await expect(page.getByTestId('login-submit')).toBeVisible();
    await shot('login');

    await page.getByTestId('login-username').fill(process.env.E2E_USER ?? 'admin@example.com');
    await page.getByTestId('login-password').fill(process.env.E2E_PASSWORD ?? 'Admin_ChangeMe_2026!');
    await page.getByTestId('login-submit').click();
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByTestId('dashboard-summary')).toBeVisible();
    await shot('dashboard');

    await navigate(page, '/branches');
    await expect(page.getByTestId('branch-add')).toBeVisible();
    await shot('branches');

    await navigate(page, '/admin/users');
    await expect(page.getByTestId('admin-create-user-open')).toBeVisible();
    await shot('admin-users');
  });
});
