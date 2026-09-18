import {test, expect} from '@playwright/test';
import {MOCK_TEST_PLAN_RESULT, TINY_PNG_BASE64} from '../helpers/mock-data';

test.describe('Screenshot analysis', () => {
  test.beforeEach(async ({page}) => {
    await page.route('**/api/history', route => route.fulfill({json: []}));
    await page.goto('/');
  });

  test('uploading a screenshot with instructions renders test cases', async ({page}) => {
    let capturedBody = '';
    await page.route('**/api/screenshot/analyze', async route => {
      capturedBody = route.request().postData() ?? '';
      await route.fulfill({json: MOCK_TEST_PLAN_RESULT});
    });

    await page.locator('#screenshot-file').setInputFiles({
      name: 'checkout.png',
      mimeType: 'image/png',
      buffer: Buffer.from(TINY_PNG_BASE64, 'base64'),
    });
    await page.locator('#screenshot-prompt').fill('Focus on the checkout form and validation errors.');
    await page.locator('#screenshot-form button[type="submit"]').click();

    await expect(page.locator('#screenshot-status')).toHaveText('Done.');
    await expect(page.locator('#result-section')).toBeVisible();
    await expect(page.locator('#result-summary')).toHaveText(MOCK_TEST_PLAN_RESULT.summary);
    expect(capturedBody).toContain('Focus on the checkout form and validation errors.');
  });

  test('surfaces the backend validation error for a non-image upload', async ({page}) => {
    await page.route('**/api/screenshot/analyze', route =>
      route.fulfill({status: 400, json: {error: 'Uploaded file must be an image (PNG, JPEG, GIF, or WebP).'}}));

    await page.locator('#screenshot-file').setInputFiles({
      name: 'notes.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('not an image'),
    });
    await page.locator('#screenshot-form button[type="submit"]').click();

    await expect(page.locator('#screenshot-status')).toHaveText(
      'Uploaded file must be an image (PNG, JPEG, GIF, or WebP).');
    await expect(page.locator('#screenshot-status')).toHaveClass(/error/);
    await expect(page.locator('#result-section')).toBeHidden();
  });
});
