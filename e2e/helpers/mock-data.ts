/** Shared fixtures for mocking /api/* responses in the e2e tests. */

export const MOCK_TEST_PLAN_RESULT = {
  summary: 'Password reset flow test plan',
  scope: 'Forgot-password request through reset confirmation',
  assumptions: [
    'User already has a verified account',
    'Reset emails are delivered within 2 minutes',
  ],
  testCases: [
    {
      id: 'TC-1',
      title: 'Request reset link with a valid, registered email',
      type: 'Functional',
      priority: 'High',
      preconditions: 'User is registered and logged out',
      steps: [
        'Navigate to /forgot-password',
        'Enter a valid registered email',
        'Submit the form',
      ],
      expectedResult: 'A confirmation message is shown and a reset email is sent',
    },
    {
      id: 'TC-2',
      title: 'Request reset link with an unregistered email',
      type: 'Negative',
      priority: 'Medium',
      preconditions: 'Email is not associated with any account',
      steps: [
        'Navigate to /forgot-password',
        'Enter an unregistered email',
        'Submit the form',
      ],
      expectedResult: 'A generic confirmation message is shown without revealing account existence',
    },
  ],
};

export const MOCK_HISTORY_SUMMARIES = [
  {
    id: 1,
    summary: 'Password reset flow test plan',
    sourceType: 'PRD',
    sourceFilename: 'password-reset-prd.md',
    createdAt: '2026-07-18T10:00:00Z',
  },
  {
    id: 2,
    summary: 'Checkout screenshot test cases',
    sourceType: 'SCREENSHOT',
    sourceFilename: 'checkout.png',
    createdAt: '2026-07-19T14:30:00Z',
  },
];

/** Minimal valid 1x1 transparent PNG, used to satisfy the backend's image content-type check. */
export const TINY_PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';
