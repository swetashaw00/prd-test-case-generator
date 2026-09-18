# API Testing Module — hands-on practice

A guided, hands-on tour of API testing technique, using this project's own live API
(`POST /api/prd/analyze`, `POST /api/screenshot/analyze`, `GET /api/history`,
`GET /api/history/{id}`) as the target. Same 10 exercises, 4 different tools — the point
isn't the specific endpoints, it's building muscle memory for how each tool approaches the
same problem.

Start the app first if it isn't already running:

```bash
cd ..
./mvnw spring-boot:run   # or mvnw.cmd on Windows
```

## The 5 methods, compared

| Method | Where requests live | Best for | Weakness |
|---|---|---|---|
| **Postman** (`postman/`) | A GUI app, `.postman_collection.json` | Manual, exploratory testing; building intuition for a new API; chaining requests via variables | Collection JSON isn't fun to code-review; state lives partly in the app |
| **Newman** (CLI) | Same Postman collection, run headlessly | Automating that *exact* collection in CI/a script — zero rewrite | Still constrained by what Postman's test syntax (Chai/JS) can express |
| **curl / raw HTTP** (`curl/requests.sh`) | Shell script | Understanding exactly what's on the wire, with no client abstraction in the way | Verbose for multipart bodies; no built-in assertions |
| **REST Client (.http)** (`http/requests.http`) | Plain text file, editor-native | Version-controlled, diffable request definitions; quick manual checks without leaving your editor | No scripting/chaining like Postman's test scripts |
| **rest-assured** (`../src/test/java/.../ApiSmokeTest.java`) | Java test code | Real assertions that run in CI, fail the build, survive refactors | Highest setup cost; not for quick exploratory pokes |

Rule of thumb: **explore in Postman or REST Client → automate the exact same checks in
Newman → once the behavior is locked in, promote it to a real rest-assured test** so it
runs on every build instead of only when a human remembers to click it.

## Setup

### Postman
1. Install [Postman](https://www.postman.com/downloads/).
2. Import `postman/PRD-Test-Case-Generator.postman_collection.json`.
3. Import `postman/PRD-Test-Case-Generator.postman_environment.json` and select it as the
   active environment (top-right environment dropdown).

### Newman (CLI)
Already wired into this repo's `package.json`:
```bash
npm install   # installs newman as a devDependency
npm run test:api:newman
```

### REST Client
Install the [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
VS Code extension, open `http/requests.http`, click "Send Request" above any `###` block.

### curl
No install needed beyond curl itself (ships with Git Bash / Windows 10+ / macOS / Linux).
```bash
bash api-testing/curl/requests.sh          # prints usage
bash api-testing/curl/requests.sh 7        # e.g. list history
```

### rest-assured (existing code-level tests)
```bash
cd ..
./mvnw test -Dtest=ApiSmokeTest
```

## The 10 exercises

Work through these in whichever tool you're practicing — the assertions and endpoints are
identical across all four, only the syntax changes. Postman folder names in parens.

| # | Exercise | Endpoint | Expect |
|---|---|---|---|
| 0 | Health check | `GET /` | `200`, `text/html` |
| 1 | Analyze a real PRD (happy path) — *(Manual)* | `POST /api/prd/analyze` | `200`, JSON with `summary`, `testCases[]` |
| 2 | Analyze with **no body/Content-Type at all** — *(PRD Analysis)* | `POST /api/prd/analyze` | `415`, not `400` — see note below |
| 3 | Analyze a real, **0-byte** file — *(PRD Analysis)* | `POST /api/prd/analyze` | `400`, error = "No file was uploaded" |
| 4 | Analyze a **1-byte whitespace-only** file — *(PRD Analysis)* | `POST /api/prd/analyze` | `400`, error mentions "extractable text" |
| 5 | Analyze a real screenshot + prompt (happy path) — *(Manual)* | `POST /api/screenshot/analyze` | `200`, JSON with `testCases[]` |
| 6 | Analyze a non-image file as a "screenshot" — *(Screenshot Analysis)* | `POST /api/screenshot/analyze` | `400`, error mentions "image" |
| 7 | List history — *(History)* | `GET /api/history` | `200`, JSON array |
| 8 | Get one history item by id (chain from #7) — *(History)* | `GET /api/history/{id}` | `200`, full result with `testCases[]` |
| 9 | Get a history item that doesn't exist — *(History)* | `GET /api/history/999999999` | `404` |

**Note on cost/time**: exercises 1 and 5 hit the real Gemini API and can take up to ~1
minute and consume free-tier quota — don't loop them; they're in the Postman collection's
"Manual (Postman GUI only)" folder and deliberately excluded from `npm run test:api:newman`.
Exercises 0, 2, 3, 4, 6, 7, 8, 9 never call Gemini, run in ~1 second total, and are exactly
what Newman automates.

**A real discovery, not a hypothetical**: exercise 2 is a good demonstration of why you test
by hand before trusting a test file. `ApiSmokeTest.analyzePrd_withNoFile_returns400()`
asserts `400` for "no file uploaded" — but sending a request with no multipart body at all
actually gets rejected at `415 Unsupported Media Type`, before the controller method (and its
`400` logic) ever runs, because the endpoint declares `consumes = "multipart/form-data"`.
Exercise 3 (a real 0-byte file, which *does* reach the controller and *does* return `400`) is
the case that test was probably trying to cover. Worth fixing `ApiSmokeTest` once you've
confirmed this yourself in Postman/curl.

**Also worth noticing**: exercises 3 and 4 look similar ("upload an empty-ish file") but hit
two different branches — Spring's `MultipartFile.isEmpty()` is `true` for a genuinely 0-byte
file (branch: "no file was uploaded"), while a 1-byte whitespace-only file passes that check
and instead fails text extraction (branch: "no extractable text"). Fixtures for both live in
`fixtures/`.

## Progression, once you've done all 10 once

1. **Chaining**: in Postman, exercise 7's test script captures `historyId` into the
   environment; exercise 8 reads it back. Try reproducing that same chain by hand in
   REST Client (copy the id manually) vs. scripted in Newman (automatic) — notice the
   difference in what "automation" buys you.
2. **Add an assertion**: pick any Postman request and add one more `pm.test(...)` — e.g.
   assert `testCases[0].steps.length > 0` on exercise 1. Re-run via Newman to confirm it's
   picked up without touching Postman again.
3. **Graduate to code**: compare your Postman test script for exercise 3 (0-byte file → 400)
   against `ApiSmokeTest.java`, then go fix the actual bug you found in exercise 2 — replace
   `analyzePrd_withNoFile_returns400()`'s assertion with the real `415`, or add a new test
   for the 0-byte case it was probably meant to cover.
