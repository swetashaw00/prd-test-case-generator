import {test, expect} from '@playwright/test';
import {MOCK_HISTORY_SUMMARIES, MOCK_TEST_PLAN_RESULT} from '../helpers/mock-data';

test.describe('History', () => {
  test('lists past generations and loads a selected one', async ({page}) => {
    await page.route('**/api/history', route => route.fulfill({json: MOCK_HISTORY_SUMMARIES}));
    await page.route(`**/api/history/${MOCK_HISTORY_SUMMARIES[0].id}`, route =>
      route.fulfill({json: MOCK_TEST_PLAN_RESULT}));

    await page.goto('/');

    const items = page.locator('#history-list li');
    await expect(items).toHaveCount(MOCK_HISTORY_SUMMARIES.length);
    await expect(page.locator('#history-empty')).toBeHidden();
    await expect(items.first()).toContainText(MOCK_HISTORY_SUMMARIES[0].summary);

    await items.first().click();

    await expect(page.locator('#result-section')).toBeVisible();
    await expect(page.locator('#result-summary')).toHaveText(MOCK_TEST_PLAN_RESULT.summary);
  });

  test('shows the empty state when there is no history', async ({page}) => {
    await page.route('**/api/history', route => route.fulfill({json: []}));

    await page.goto('/');

    await expect(page.locator('#history-empty')).toBeVisible();
    await expect(page.locator('#history-list li')).toHaveCount(0);
  });
});
