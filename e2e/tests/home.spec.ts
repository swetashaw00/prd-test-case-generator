import {test, expect} from '@playwright/test';

test.describe('Home page', () => {
  test.beforeEach(async ({page}) => {
    await page.route('**/api/history', route => route.fulfill({json: []}));
  });

  test('renders both upload forms and an empty history state', async ({page}) => {
    await page.goto('/');

    await expect(page.getByRole('heading', {name: 'PRD & Screenshot Test Case Generator'})).toBeVisible();
    await expect(page.locator('#prd-form')).toBeVisible();
    await expect(page.locator('#prd-file')).toBeVisible();
    await expect(page.locator('#screenshot-form')).toBeVisible();
    await expect(page.locator('#screenshot-file')).toBeVisible();
    await expect(page.locator('#history-empty')).toBeVisible();
    await expect(page.locator('#result-section')).toBeHidden();
  });
});
