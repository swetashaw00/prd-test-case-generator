# PRD: Google Business Profile SEO Optimization Score & Recommendations

## Summary

Local businesses often under-optimize their Google Business Profile (GBP) — incomplete
categories, missing hours, stale photos, unanswered Q&A, low review-response rates — which
directly hurts local map-pack ranking and organic visibility. This feature gives every connected
GBP location a single **SEO Optimization Score** (0–100) plus a prioritized list of concrete,
actionable recommendations to raise it, so marketing teams know exactly what to fix first instead
of guessing.

## Goals

- Score every connected GBP location's SEO/marketing health on a consistent, explainable 0–100
  scale, refreshed on a regular schedule.
- Turn each scoring gap into one specific, actionable recommendation (not a vague "improve your
  profile" message).
- Let a user apply a recommendation (where the API supports it) directly from the dashboard, or
  mark it as "won't fix" with a reason.
- Surface score trend over time so a location manager can see whether their changes actually
  moved the needle.

## Non-Goals

- This feature does not compute or influence actual Google Maps ranking position — that's a
  separate ranking/heatmap product. The score is a proxy for profile health, not a live rank
  prediction.
- Multi-location bulk editing across an enterprise's entire location set is out of scope for v1;
  each location is scored and acted on individually.
- This does not generate or publish new GBP posts automatically — that's covered by the separate
  Posts feature.

## User Story

As a local SEO manager responsible for one or more Google Business Profiles, I want a clear score
and prioritized to-do list for each location so I can quickly identify and fix the specific gaps
hurting that location's local SEO performance, without manually auditing every field myself.

## Scoring Categories

The SEO Optimization Score is a weighted sum across these categories:

| Category | Weight | Example checks |
|---|---|---|
| Profile completeness | 25% | Primary + secondary categories set, business description present and keyword-relevant, attributes filled, service area or address configured correctly |
| Photos & media | 15% | Minimum photo count met, cover photo set, photos added within the last 90 days |
| Posts activity | 10% | At least one GBP post published in the last 30 days |
| Q&A management | 10% | No unanswered questions older than 7 days |
| Review engagement | 20% | Review response rate over the last 90 days, average response time |
| Hours & special hours | 10% | Regular hours set, holiday/special hours kept current |
| NAP consistency | 10% | Name/Address/Phone matches the citation network within tolerance |

## Flow

1. On a scheduled cadence (daily) and on-demand ("Refresh score" button), the system pulls the
   latest GBP data for a location and recomputes its score.
2. The dashboard shows the location's current score, its trend over the last 90 days, and a
   category breakdown.
3. Below the score, a prioritized recommendation list shows the highest-impact gaps first (largest
   weighted-category shortfall first).
4. Each recommendation shows: what's wrong, why it matters (one sentence, plain language, no
   jargon), and an action — either an "Apply" button (for API-supported fixes, e.g. filling a
   missing attribute) or "See how" instructions (for fixes GBP's API can't perform directly, e.g.
   uploading a specific photo).
5. A user can dismiss a recommendation with a required reason (e.g. "not applicable to this
   business"); dismissed recommendations don't count against the score for 90 days, after which
   they're re-evaluated.
6. Applying a recommendation triggers an immediate partial score recompute for the affected
   category, without waiting for the next full daily refresh.

## Requirements

- The score must be deterministic for identical input data — recomputing on the same profile
  state without any changes must always produce the same score.
- Each of the 7 category weights must sum to exactly 100%, and this must be enforced/validated at
  configuration time, not just assumed.
- A location with no reviews at all must not be unfairly penalized to zero on the Review
  Engagement category — the response-rate calculation must exclude locations with fewer than 3
  total reviews from that specific sub-metric, falling back to a neutral score for that sub-metric.
- Recommendations must be ranked by weighted-impact (category weight × the size of the gap in that
  category), not by category order or alphabetically.
- "Apply" actions must confirm success or failure back to the user within 10 seconds, or show a
  pending/in-progress state if the underlying GBP API call is asynchronous.
- A failed "Apply" action must leave the recommendation in its original (not-yet-applied) state
  and show a clear, specific error — not a generic "something went wrong."
- Dismissed recommendations must reappear automatically after 90 days if the underlying gap still
  exists, and must not reappear if the gap was independently resolved in the meantime.
- The score and its category breakdown must be visible to a user who only has read access (no
  ability to apply/dismiss), without exposing the "Apply" controls to them.
- Historical score data must be retained for at least 12 months to support trend charts.

## Out of Scope / Assumptions

- We assume the GBP OAuth connection for a given location is already valid; handling expired/
  revoked OAuth tokens is covered by the existing connection-health feature, not this one.
- Review content sentiment analysis is out of scope — only response rate/time is scored, not
  review quality or star rating.
- The exact numeric thresholds per category (e.g. "at least how many photos") are configurable by
  an internal admin and may change after launch; this PRD defines the mechanism, not the final
  tuned values.

## Acceptance Criteria

- [ ] Every connected GBP location shows a score between 0 and 100 with a visible category
      breakdown.
- [ ] Recommendations are ordered by weighted impact, highest first.
- [ ] A location with zero reviews does not receive an unfairly low overall score purely because
      of the review-response sub-metric.
- [ ] Applying a supported recommendation updates that category's score without requiring a full
      page refresh or waiting for the next daily job.
- [ ] Dismissing a recommendation requires a reason and suppresses it for exactly 90 days, then
      re-evaluates it.
- [ ] A read-only user can see the score and recommendations but never sees an "Apply" or
      "Dismiss" control.
- [ ] Score history for a location is queryable for at least the trailing 12 months.
- [ ] A failed "Apply" call shows a specific error message and does not silently mark the
      recommendation as resolved.
