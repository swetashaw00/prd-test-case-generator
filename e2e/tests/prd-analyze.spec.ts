import {test, expect} from '@playwright/test';
import {MOCK_TEST_PLAN_RESULT} from '../helpers/mock-data';

test.describe('PRD analysis', () => {
  test.beforeEach(async ({page}) => {
    await page.route('**/api/history', route => route.fulfill({json: []}));
    await page.goto('/');
  });

  test('uploading a PRD renders the generated test plan', async ({page}) => {
    await page.route('**/api/prd/analyze', route => route.fulfill({json: MOCK_TEST_PLAN_RESULT}));

    await page.locator('#prd-file').setInputFiles({
      name: 'sample-prd.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('# Sample PRD\n\nUsers can reset their password via email.'),
    });
    await page.locator('#prd-form button[type="submit"]').click();

    await expect(page.locator('#prd-status')).toHaveText('Done.');
    await expect(page.locator('#prd-status')).not.toHaveClass(/error/);
    await expect(page.locator('#result-section')).toBeVisible();
    await expect(page.locator('#result-summary')).toHaveText(MOCK_TEST_PLAN_RESULT.summary);
    await expect(page.locator('#result-scope')).toHaveText(MOCK_TEST_PLAN_RESULT.scope);
    await expect(page.locator('#result-body tr')).toHaveCount(MOCK_TEST_PLAN_RESULT.testCases.length);

    const firstRow = page.locator('#result-body tr').first();
    await expect(firstRow.locator('td').nth(1)).toHaveText(MOCK_TEST_PLAN_RESULT.testCases[0].title);
    await expect(firstRow.locator('.steps li')).toHaveCount(MOCK_TEST_PLAN_RESULT.testCases[0].steps.length);
  });

  test('shows the backend error message when analysis fails', async ({page}) => {
    await page.route('**/api/prd/analyze', route =>
      route.fulfill({status: 400, json: {error: 'No extractable text was found in the uploaded document.'}}));

    await page.locator('#prd-file').setInputFiles({
      name: 'empty.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from(''),
    });
    await page.locator('#prd-form button[type="submit"]').click();

    await expect(page.locator('#prd-status')).toHaveText('No extractable text was found in the uploaded document.');
    await expect(page.locator('#prd-status')).toHaveClass(/error/);
    await expect(page.locator('#result-section')).toBeHidden();
  });
});
