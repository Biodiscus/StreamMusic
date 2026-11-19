#!/usr/bin/env bash
# Builds the signed release app bundle (.aab) for the Play Store.
# Signing details come from the files made by scripts/create-release-secrets.sh.
# Usage: scripts/build-release-bundle.sh [secrets-directory]   (default: scripts/release-secrets)
set -euo pipefail

SCRIPTS_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIRECTORY="$(dirname "$SCRIPTS_DIRECTORY")"
SECRETS_DIRECTORY="${1:-$SCRIPTS_DIRECTORY/release-secrets}"
BUNDLE_FILE="app/build/outputs/bundle/release/app-release.aab"

read_secret() {
  local secret_name="$1"
  local secret_file="$SECRETS_DIRECTORY/$secret_name.txt"
  if [ ! -f "$secret_file" ]; then
    echo "Missing $secret_file. Run scripts/create-release-secrets.sh first." >&2
    exit 1
  fi
  cat "$secret_file"
}

cd "$PROJECT_DIRECTORY"

export RELEASE_KEYSTORE_PATH="$SECRETS_DIRECTORY/release.jks"
export RELEASE_KEYSTORE_PASSWORD="$(read_secret RELEASE_KEYSTORE_PASSWORD)"
export RELEASE_KEY_ALIAS="$(read_secret RELEASE_KEY_ALIAS)"
export RELEASE_KEY_PASSWORD="$(read_secret RELEASE_KEY_PASSWORD)"

eval "$("$SCRIPTS_DIRECTORY/app-version.sh")"
echo "Building version $VERSION_NAME (code $VERSION_CODE)"

./gradlew bundleRelease -PversionName="$VERSION_NAME" -PversionCode="$VERSION_CODE"
./gradlew --stop

echo "Done: $PROJECT_DIRECTORY/$BUNDLE_FILE"
