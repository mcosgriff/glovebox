#!/usr/bin/env bash
# Builds the one-off APK that proves to Google that this project holds the release key (#267, Android
# developer verification). Run it on the maintainer's machine, with the release keystore; nothing here
# runs in CI and the key goes nowhere new.
#
#   .github/scripts/registration-apk.sh <snippet-file> <keystore.jks>
#
# <snippet-file> holds the text the Android Developer Console gives under "Upload APK" when a package
# name or key is registered. The script puts it in app/src/main/assets/adi-registration.properties for
# one release build, signed with the keystore, then removes it again (git ignores that file, so it is
# never committed). The APK is checked to be signed with the key whose fingerprint the README publishes,
# and to carry the snippet, and lands in build/registration/. Upload it to the console only: it is not a
# release and must not be published, and it is never installed on a phone.
#
# The keystore passwords and alias come from GLOVEBOX_KEYSTORE_PASSWORD, GLOVEBOX_KEY_ALIAS (default
# "glovebox") and GLOVEBOX_KEY_PASSWORD, or are asked for.
set -euo pipefail

# The release certificate's SHA-256, as in the README's "Verify your download".
EXPECTED="7d6101c4f22719fe4f801783bd976b5d36341961a5ac6abc87597add00875ef7"

if [ $# -ne 2 ]; then
  sed -n '5,6p' "$0" | sed 's/^# //'
  exit 2
fi
snippet=$(realpath "$1")
keystore=$(realpath "$2")
root=$(cd "$(dirname "$0")/../.." && pwd)
asset="$root/app/src/main/assets/adi-registration.properties"
out="$root/build/registration"

[ -s "$snippet" ] || { echo "No snippet in $snippet." >&2; exit 1; }
[ -f "$keystore" ] || { echo "No keystore at $keystore." >&2; exit 1; }
if [ -e "$asset" ]; then
  echo "$asset already exists, from an earlier run that didn't finish? Check it and delete it first." >&2
  exit 1
fi

apksigner=$(ls -d "${ANDROID_HOME:-$HOME/Android/Sdk}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)
[ -x "$apksigner" ] || { echo "apksigner not found; set ANDROID_HOME." >&2; exit 1; }

if [ -z "${GLOVEBOX_KEYSTORE_PASSWORD:-}" ]; then
  read -rsp "Keystore password: " GLOVEBOX_KEYSTORE_PASSWORD; echo
fi
GLOVEBOX_KEY_ALIAS=${GLOVEBOX_KEY_ALIAS:-glovebox}
if [ -z "${GLOVEBOX_KEY_PASSWORD:-}" ]; then
  read -rsp "Key password (Enter if the same): " GLOVEBOX_KEY_PASSWORD; echo
  GLOVEBOX_KEY_PASSWORD=${GLOVEBOX_KEY_PASSWORD:-$GLOVEBOX_KEYSTORE_PASSWORD}
fi
export GLOVEBOX_KEYSTORE="$keystore" GLOVEBOX_KEYSTORE_PASSWORD GLOVEBOX_KEY_ALIAS GLOVEBOX_KEY_PASSWORD

# Whatever happens next, the snippet doesn't stay in the source tree.
trap 'rm -f "$asset"' EXIT
cp "$snippet" "$asset"

(cd "$root" && ./gradlew --quiet :app:assembleRelease)

apk="$root/app/build/outputs/apk/release/app-release.apk"
mkdir -p "$out"
cp "$apk" "$out/glovebox-registration.apk"
apk="$out/glovebox-registration.apk"

signer=$("$apksigner" verify --print-certs "$apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
if [ "$signer" != "$EXPECTED" ]; then
  rm -f "$apk"
  echo "The APK is signed with $signer, not the release key ($EXPECTED). Wrong keystore or alias?" >&2
  exit 1
fi
if ! unzip -p "$apk" assets/adi-registration.properties | cmp -s - "$snippet"; then
  rm -f "$apk"
  echo "The APK doesn't carry the snippet as given." >&2
  exit 1
fi

# The next build mustn't pick up anything from this one.
(cd "$root" && ./gradlew --quiet :app:clean)

echo "Signed with the release key and carrying the snippet: $apk"
echo "Upload it in the Android Developer Console, then delete it. Don't publish or install it."
