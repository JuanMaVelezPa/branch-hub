import { expect, test } from '@playwright/test';

test('smoke: login -> dashboard -> list -> create branch', async ({ page }) => {
  const uniqueCode = `BR-${Date.now()}`;

  await page.goto('/login');

  await page.getByTestId('login-username').fill(process.env.E2E_USER ?? 'admin@example.com');
  await page.getByTestId('login-password').fill(process.env.E2E_PASSWORD ?? 'Admin_ChangeMe_2026!');
  await page.getByTestId('login-submit').click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByTestId('dashboard-summary')).toBeVisible();

  await page.getByTestId('nav-branches').click();
  await expect(page).toHaveURL(/\/branches$/);
  await expect(page.getByRole('heading', { name: /branches/i })).toBeVisible();

  await page.getByTestId('branch-add').click();
  await expect(page).toHaveURL(/\/branches\/new$/);

  await page.getByTestId('branch-code').fill(uniqueCode);
  await page.getByTestId('branch-name').fill(`Smoke ${uniqueCode}`);
  await page.getByTestId('branch-city').fill('Bogota');
  await page.getByTestId('branch-submit').click();

  await expect(page).toHaveURL(/\/branches$/);
  await expect(page.getByRole('cell', { name: uniqueCode, exact: true })).toBeVisible();

  await page.getByRole('link', { name: uniqueCode, exact: true }).click();
  await expect(page.getByTestId('branch-detail-code')).toHaveText(uniqueCode);

  await page.getByTestId('branch-detail-edit').click();
  await expect(page).toHaveURL(/\/branches\/[0-9a-f-]{36}\/edit$/i);

  await page.getByTestId('branch-edit-city').fill('Medellin');
  await page.getByTestId('branch-edit-submit').click();
  await expect(page).toHaveURL(/\/branches\/[0-9a-f-]{36}$/i);
  await expect(page.getByText('Medellin')).toBeVisible();

  await page.getByTestId('branch-detail-deactivate').click();
  await expect(page.getByTestId('branch-detail-deactivate-confirm')).toBeVisible();
  await page.getByTestId('branch-detail-deactivate-confirm').click();
  await expect(page.getByTestId('branch-detail-deactivate')).toHaveCount(0);
});
