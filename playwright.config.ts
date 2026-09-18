import {defineConfig, devices} from '@playwright/test';

/**
 * The webServer boots the real Spring Boot app so the mocked tests below
 * exercise the actual static UI + JS. No GEMINI_API_KEY is required to boot —
 * the key is only read per-request by GeminiTestCaseService, and every
 * /api/* call in these tests is intercepted via page.route() before it
 * reaches the network.
 */
export default defineConfig({
  testDir: './e2e/tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI ? [['html'], ['list']] : 'html',

  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  webServer: {
    command: process.platform === 'win32' ? 'mvnw.cmd spring-boot:run' : './mvnw spring-boot:run',
    url: 'http://localhost:8080',
    reuseExistingServer: true,
    timeout: 120_000,
  },

  projects: [
    {name: 'chromium', use: {...devices['Desktop Chrome']}},
  ],
});
