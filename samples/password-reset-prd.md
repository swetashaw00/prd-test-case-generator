# PRD: Self-Service Password Reset

## Summary

Users who forget their password must be able to regain access to their account without
contacting support. This document specifies the "Forgot Password" flow for the web app.

## Goals

- Let a user reset their password using only their registered email address.
- Keep the flow secure against account enumeration and brute-force abuse.
- Keep the flow accessible and usable on both desktop and mobile.

## Non-Goals

- SMS-based or authenticator-app-based password reset (future work).
- Password reset for SSO-only accounts (they must reset via their identity provider).

## User Story

As a user who has forgotten my password, I want to request a reset link by email so that I can
set a new password and log back in.

## Flow

1. User clicks "Forgot password?" on the login page.
2. User enters their email address and submits the form.
3. The system always shows the same confirmation message ("If an account exists for that email,
   we've sent a reset link") regardless of whether the email is registered, to prevent account
   enumeration.
4. If the email is registered, the system emails a reset link containing a single-use token.
5. The user clicks the link and is taken to a "Set a new password" page.
6. The user enters and confirms a new password meeting the password policy.
7. On success, the token is invalidated, the password is updated, and the user is redirected to
   the login page with a success message.

## Requirements

- The reset token must expire 30 minutes after it is issued.
- The reset token must be single-use — reusing a consumed or expired token must show an error
  and require the user to request a new link.
- A user may request at most 3 reset emails per hour for the same email address; further
  requests within that window must be silently rate-limited (still show the generic confirmation
  message, but do not send another email).
- The new password must satisfy the existing password policy: minimum 10 characters, at least
  one number, and at least one special character.
- The "new password" and "confirm password" fields must match exactly before the form can be
  submitted.
- All existing sessions for the account must be invalidated once the password is successfully
  reset.
- The reset request form and the new-password form must both be fully usable via keyboard
  only, and all form fields must have accessible labels.
- The reset link must only be usable over HTTPS.

## Out of Scope / Assumptions

- Email deliverability (e.g., spam filtering) is handled by the transactional email provider and
  is not covered by this PRD.
- The password policy itself is defined elsewhere and is referenced, not redefined, here.

## Acceptance Criteria

- [ ] Submitting a registered email sends exactly one reset email with a valid, single-use link.
- [ ] Submitting an unregistered email shows the same generic confirmation message and sends no email.
- [ ] A 4th reset request for the same email within one hour does not send a 4th email.
- [ ] A reset link older than 30 minutes shows an "expired link" error, not a working reset form.
- [ ] Successfully resetting the password logs the user out of all other active sessions.
- [ ] A password that doesn't meet the policy is rejected with a clear inline error before submission.
- [ ] Mismatched "new password" / "confirm password" fields block submission with a clear error.
- [ ] Both forms are fully operable using only a keyboard, with visible focus states.
