#!/bin/sh
# Copies the backend's OpenAPI spec into the tests, so ApiContractTest checks
# the app against the current API. Run after changing endpoints in MMD_API
# (and running `php artisan scramble:export` there).
set -e
API_REPO="${1:-$HOME/Documents/MMD_API}"
cp "$API_REPO/docs/openapi.json" "$(dirname "$0")/../app/src/test/resources/openapi.json"
echo "Copied $API_REPO/docs/openapi.json ($(cd "$API_REPO" && git log -1 --format='%h %s'))"
