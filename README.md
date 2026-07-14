# PRD & Screenshot Test Case Generator

A Java (Spring Boot) web app that uses Claude (Anthropic API) to turn a PRD document or a UI
screenshot into a structured test plan and a list of concrete test cases.

- Upload a PRD (`.pdf`, `.docx`, `.txt`, `.md`) → get a test plan + test cases.
- Upload a UI screenshot (PNG/JPEG/GIF/WebP) → get test cases generated from what's visible in the image.

## Requirements

- Java 17+
- An Anthropic API key

No local Maven install is required — the project ships the Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Setup

Set your Anthropic API key as an environment variable (never hardcode it):

```bash
# macOS/Linux
export ANTHROPIC_API_KEY=sk-ant-...

# Windows PowerShell
$env:ANTHROPIC_API_KEY = "sk-ant-..."
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
| `anthropic.model` | `claude-opus-4-8` | Claude model used for generation |
| `anthropic.max-tokens` | `8192` | Max output tokens per generation request |
| `server.port` | `8080` | HTTP port |
| `spring.servlet.multipart.max-file-size` | `25MB` | Max upload size |

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

## Architecture

- `service/DocumentExtractionService` — extracts plain text from PDF (Apache PDFBox) and DOCX (Apache POI).
- `service/ClaudeTestCaseService` — builds the Claude prompt (text or vision), calls the Messages API via the
  official `anthropic-java` SDK, and parses the JSON response.
- `web/TestGenController` — REST endpoints, file validation, and error mapping from Claude API errors to HTTP
  responses.
- `config/ClaudeConfig` — the `AnthropicClient` bean, configured from the `ANTHROPIC_API_KEY` environment variable.
- `resources/static/` — a small vanilla-JS single-page UI (no separate frontend build).
