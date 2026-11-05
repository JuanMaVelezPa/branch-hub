import { expect, test } from '@playwright/test';

test('smoke: login -> admin users -> create user', async ({ page }) => {
  const uniqueEmail = `smoke-admin-${Date.now()}@example.com`;

  await page.goto('/login');
  await page.getByTestId('login-username').fill(process.env.E2E_USER ?? 'admin@example.com');
  await page.getByTestId('login-password').fill(process.env.E2E_PASSWORD ?? 'Admin_ChangeMe_2026!');
  await page.getByTestId('login-submit').click();

  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByTestId('nav-admin').click();
  await expect(page).toHaveURL(/\/admin\/users$/);

  await page.getByTestId('admin-create-user-open').click();
  await expect(page.getByTestId('admin-create-user-modal')).toBeVisible();

  await page.getByTestId('admin-create-user-email').fill(uniqueEmail);
  await page.getByTestId('admin-create-user-password').fill('Tmp_ChangeMe_2026!');

  // Default role is APP_ADMIN; keep it as-is.
  await page.getByTestId('admin-create-user-submit').click();

  await expect(page.getByText(uniqueEmail, { exact: true })).toBeVisible();
});

