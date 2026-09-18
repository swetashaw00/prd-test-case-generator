# PRD & Screenshot Test Case Generator

A Java (Spring Boot) web app that uses Google Gemini (free tier) to turn a PRD document or a UI
screenshot into a structured test plan and a list of concrete test cases.

- Upload a PRD (`.pdf`, `.docx`, `.txt`, `.md`) → get a test plan + test cases.
- Upload a UI screenshot (PNG/JPEG/GIF/WebP) → get test cases generated from what's visible in the image.

## Requirements

- Java 17+
- A free Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)

No local Maven install is required — the project ships the Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Setup

Copy `.env.example` to `.env` and set your key there:

```bash
cp .env.example .env
# then edit .env and set GEMINI_API_KEY=AIza...
```

`.env` is gitignored and is loaded automatically on startup (see
`PrdTestCaseGeneratorApplication.loadDotEnv()`) — never commit it or paste a real key into
`.env.example`.

Alternatively, set it as a real environment variable instead (takes precedence over `.env`):

```bash
# macOS/Linux
export GEMINI_API_KEY=AIza...

# Windows PowerShell
$env:GEMINI_API_KEY = "AIza..."
```

## Run

```bash
./mvnw spring-boot:run
```

Then open http://localhost:8080 in a browser.

## Configuration

Override in `src/main/resources/application.properties` or via `-D` / env vars:

| Property | Default | Purpose |
|---|---|---|
| `gemini.model` | `gemini-3-flash-preview` | Gemini model used for generation — see the comment above this property in `application.properties` for why this specific model, and what to check if it stops working |
| `gemini.max-tokens` | `8192` | Max output tokens per generation request |
| `server.port` | `8080` | HTTP port |
| `spring.servlet.multipart.max-file-size` | `25MB` | Max upload size |
| `spring.datasource.url` | `jdbc:h2:file:./data/prdtestgen` | H2 database file location (generation history) |
| `spring.jpa.hibernate.ddl-auto` | `update` | Auto-creates/updates history tables on startup |

## API

### `POST /api/prd/analyze`

Multipart form field `file` — a `.pdf`, `.docx`, `.txt`, or `.md` PRD document.

### `POST /api/screenshot/analyze`

Multipart form fields:
- `file` — an image (PNG/JPEG/GIF/WebP)
- `prompt` (optional) — extra instructions, e.g. "Focus on the checkout form."

Both endpoints return JSON:

```json
{
  "summary": "...",
  "scope": "...",
  "assumptions": ["..."],
  "testCases": [
    {
      "id": "TC-001",
      "title": "...",
      "type": "Functional | Negative | Edge Case | UI | Regression | Accessibility | Performance",
      "priority": "High | Medium | Low",
      "preconditions": "...",
      "steps": ["...", "..."],
      "expectedResult": "..."
    }
  ]
}
```

On success, both endpoints also persist the result as a history entry (best-effort — a persistence
failure is logged but never fails the request).

### `GET /api/history`

Returns a lightweight list of past generations, newest first: `[{"id", "createdAt", "sourceType",
"sourceFilename", "summary"}, ...]`.

### `GET /api/history/{id}`

Returns the full result for one past generation, in the same shape as the analyze endpoints above.
`404` if the id doesn't exist.

## History & database

Every successful generation is saved to a local H2 file database (`./data/prdtestgen.mv.db`,
gitignored) via Spring Data JPA. The existing single-page UI has a "History" list — click any past
entry to re-render its result in the same table, no new page or framework involved.

Browse the raw data at http://localhost:8080/h2-console while the app is running — JDBC URL
`jdbc:h2:file:./data/prdtestgen`, user `sa`, blank password.

Schema (auto-created by `spring.jpa.hibernate.ddl-auto=update`):
- `generation_run` — one row per analysis (timestamp, source type/filename, summary, scope)
- `generation_run_assumption` — the assumptions list for a run
- `test_case_record` — the generated test cases for a run
- `test_case_step` — the steps list for a test case

## Testing

### Unit / integration tests (Java)

```bash
./mvnw test
```

Includes `ApiSmokeTest` (endpoint-level checks via rest-assured) and
`GenerationHistoryServiceTest`.

### End-to-end tests (Playwright)

`e2e/` drives the real static UI against a running Spring Boot instance, with
`/api/*` calls intercepted via `page.route()` — no `GEMINI_API_KEY` needed to
run these.

```bash
npm install
npx playwright install chromium   # first run only
npm run test:e2e           # headless
npm run test:e2e:ui        # interactive UI mode
npm run test:e2e:headed    # headed browser
```

`playwright.config.ts` boots the app itself (`mvnw spring-boot:run`) against
`http://localhost:8080` if nothing is already listening there.

### API testing exercises

`api-testing/` is a guided, hands-on comparison of five ways to test the same
API (Postman, Newman, curl, VS Code REST Client, rest-assured) — see
[`api-testing/README.md`](api-testing/README.md) for the walkthrough. Quick
start:

```bash
npm install
npm run test:api:newman
```

## Architecture

- `service/DocumentExtractionService` — extracts plain text from PDF (Apache PDFBox) and DOCX (Apache POI).
- `service/GeminiTestCaseService` — builds the Gemini prompt (text or vision) and JSON response schema, calls the
  `generateContent` REST endpoint directly via Spring's `RestClient`, and parses the structured JSON response.
- `service/GenerationHistoryService` — saves a completed generation and converts stored entities back into the
  same `TestPlanResult` shape the UI already knows how to render.
- `web/TestGenController` — REST endpoints, file validation, and error mapping from Gemini API errors to HTTP
  responses (API keys are redacted from any error text).
- `config/GeminiConfig` — the `RestClient` bean used to call the Gemini API.
- `model/GenerationRun`, `model/TestCaseRecord` — JPA entities backing the history feature.
- `resources/static/` — a small vanilla-JS single-page UI (no separate frontend build).
