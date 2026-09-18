#!/usr/bin/env bash
# PRD & Screenshot Test Case Generator — curl cheatsheet.
# Run individual blocks by copy-pasting them into a shell (Git Bash on Windows
# works fine — curl.exe ships with Windows 10+). Companion to ../README.md.
#
# Usage: bash api-testing/curl/requests.sh <exercise-number>
# With no argument, prints this help.

set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# On Git Bash/MSYS, curl.exe is the native Windows binary and can't read MSYS-style
# paths like /d/foo — convert to D:/foo. On real Linux/macOS, cygpath doesn't exist,
# so the path is already native and passed through unchanged.
winpath() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi
}

exercise_0_health() {
  echo "GET $BASE_URL/"
  curl -s -o /dev/null -w "status: %{http_code}\n" "$BASE_URL/"
}

exercise_1_prd_happy_path() {
  echo "POST $BASE_URL/api/prd/analyze  (real Gemini call, can take ~1 min)"
  curl -s -X POST "$BASE_URL/api/prd/analyze" \
    -F "file=@$(winpath "$REPO_ROOT/samples/password-reset-prd.md");type=text/markdown" | python3 -m json.tool
}

exercise_2_prd_no_body() {
  echo "POST $BASE_URL/api/prd/analyze  (no body/Content-Type at all -> expect 415, not 400 —"
  echo "  consumes=\"multipart/form-data\" rejects a non-multipart request before the controller runs)"
  curl -s -X POST "$BASE_URL/api/prd/analyze" -w "\nstatus: %{http_code}\n"
}

exercise_3_prd_zero_byte_file() {
  echo "POST $BASE_URL/api/prd/analyze  (real multipart part, 0 bytes -> expect 400, 'No file was uploaded')"
  curl -s -X POST "$BASE_URL/api/prd/analyze" \
    -F "file=@$(winpath "$REPO_ROOT/api-testing/fixtures/empty.txt");type=text/plain" -w "\nstatus: %{http_code}\n"
}

exercise_4_prd_whitespace_only_file() {
  echo "POST $BASE_URL/api/prd/analyze  (1-byte whitespace file -> expect 400, 'extractable text')"
  curl -s -X POST "$BASE_URL/api/prd/analyze" \
    -F "file=@$(winpath "$REPO_ROOT/api-testing/fixtures/blank.txt");type=text/plain" -w "\nstatus: %{http_code}\n"
}

exercise_5_screenshot_happy_path() {
  local image_path="${1:?usage: exercise_5_screenshot_happy_path /path/to/screenshot.png}"
  echo "POST $BASE_URL/api/screenshot/analyze  (real Gemini vision call)"
  curl -s -X POST "$BASE_URL/api/screenshot/analyze" \
    -F "file=@$image_path" \
    -F "prompt=Focus on the checkout form and validation errors." | python3 -m json.tool
}

exercise_6_screenshot_non_image() {
  echo "POST $BASE_URL/api/screenshot/analyze  (.md instead of an image -> expect 400)"
  curl -s -X POST "$BASE_URL/api/screenshot/analyze" \
    -F "file=@$(winpath "$REPO_ROOT/samples/password-reset-prd.md");type=text/markdown" -w "\nstatus: %{http_code}\n"
}

exercise_7_list_history() {
  echo "GET $BASE_URL/api/history"
  curl -s "$BASE_URL/api/history" | python3 -m json.tool
}

exercise_8_get_history_item() {
  local id="${1:?usage: exercise_8_get_history_item <id>  (get one from exercise_7 first)}"
  echo "GET $BASE_URL/api/history/$id"
  curl -s "$BASE_URL/api/history/$id" -w "\nstatus: %{http_code}\n" | python3 -m json.tool
}

exercise_9_get_history_item_404() {
  echo "GET $BASE_URL/api/history/999999999  (expect 404)"
  curl -s -o /dev/null -w "status: %{http_code}\n" "$BASE_URL/api/history/999999999"
}

case "${1:-}" in
  0) exercise_0_health ;;
  1) exercise_1_prd_happy_path ;;
  2) exercise_2_prd_no_body ;;
  3) exercise_3_prd_zero_byte_file ;;
  4) exercise_4_prd_whitespace_only_file ;;
  5) exercise_5_screenshot_happy_path "${2:-}" ;;
  6) exercise_6_screenshot_non_image ;;
  7) exercise_7_list_history ;;
  8) exercise_8_get_history_item "${2:-}" ;;
  9) exercise_9_get_history_item_404 ;;
  *)
    cat <<EOF
Usage: bash api-testing/curl/requests.sh <exercise-number> [arg]

  0  health check
  1  analyze PRD, happy path                  (real Gemini call)
  2  analyze PRD, no body at all (415)
  3  analyze PRD, 0-byte file (400)
  4  analyze PRD, whitespace-only file (400)
  5  analyze screenshot, happy path            (needs an image path arg, real Gemini call)
  6  analyze screenshot, non-image upload (400)
  7  list history
  8  get history item by id                   (needs an id arg, from exercise 7)
  9  get history item, unknown id (404)
EOF
    ;;
esac
