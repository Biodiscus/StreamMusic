#!/usr/bin/env bash
# Creates a release keystore and one file per CI secret, ready to copy and paste.
# Usage: scripts/create-release-secrets.sh [output-directory]   (default: release-secrets)
set -euo pipefail

OUTPUT_DIRECTORY="${1:-release-secrets}"
KEY_ALIAS="streammusic"
KEYSTORE_FILE="$OUTPUT_DIRECTORY/release.jks"

generate_password() {
  openssl rand -hex 16
}

write_secret_file() {
  local secret_name="$1"
  local secret_value="$2"
  printf '%s' "$secret_value" > "$OUTPUT_DIRECTORY/$secret_name.txt"
}

if [ -e "$KEYSTORE_FILE" ]; then
  echo "$KEYSTORE_FILE already exists. Refusing to overwrite a keystore." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIRECTORY"
chmod 700 "$OUTPUT_DIRECTORY"

# PKCS12 keystores (the keytool default) require the key password to equal the keystore password.
KEYSTORE_PASSWORD="$(generate_password)"
KEY_PASSWORD="$KEYSTORE_PASSWORD"

keytool -genkeypair \
  -keystore "$KEYSTORE_FILE" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=StreamMusic"

write_secret_file RELEASE_KEYSTORE_BASE64 "$(base64 -w0 "$KEYSTORE_FILE")"
write_secret_file RELEASE_KEYSTORE_PASSWORD "$KEYSTORE_PASSWORD"
write_secret_file RELEASE_KEY_ALIAS "$KEY_ALIAS"
write_secret_file RELEASE_KEY_PASSWORD "$KEY_PASSWORD"

echo "Done. In $OUTPUT_DIRECTORY/:"
echo "  release.jks                       the keystore itself, back it up"
echo "  RELEASE_KEYSTORE_BASE64.txt       paste into the secret with the same name"
echo "  RELEASE_KEYSTORE_PASSWORD.txt     paste into the secret with the same name"
echo "  RELEASE_KEY_ALIAS.txt             paste into the secret with the same name"
echo "  RELEASE_KEY_PASSWORD.txt          paste into the secret with the same name"
echo "Never commit this directory. Losing release.jks means you can never update the app."
