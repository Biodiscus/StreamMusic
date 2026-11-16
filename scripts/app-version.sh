#!/usr/bin/env bash
# Prints the app version as KEY=VALUE lines: major.minor from the VERSION file, patch = number of commits.
# Needs full git history. Usage in CI: scripts/app-version.sh >> "$GITHUB_ENV"
set -euo pipefail

MAJOR_MINOR="$(tr -d '[:space:]' < VERSION)"
COMMIT_COUNT="$(git rev-list --count HEAD)"

echo "VERSION_NAME=$MAJOR_MINOR.$COMMIT_COUNT"
echo "VERSION_CODE=$COMMIT_COUNT"
