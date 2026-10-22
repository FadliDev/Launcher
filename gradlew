#!/usr/bin/env sh
# Lightweight Gradle launcher: uses Gradle from PATH when available; otherwise bootstraps
# the distribution from gradle/wrapper/gradle-wrapper.properties without a wrapper JAR.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$APP_HOME" "$@"
fi
PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
URL=$(sed -n 's/^distributionUrl=//p' "$PROPS" | sed 's/\\:/:/g' | tr -d '\r')
VERSION=$(printf '%s' "$URL" | sed -n 's#.*gradle-\([0-9][0-9.]*\)-bin\.zip#\1#p')
if [ -z "$VERSION" ]; then echo "Unable to read Gradle distribution version from $PROPS" >&2; exit 1; fi
GH=${GRADLE_USER_HOME:-"$HOME/.gradle"}
DEST="$GH/modforge/gradle-$VERSION"
if [ ! -x "$DEST/bin/gradle" ]; then
  command -v curl >/dev/null 2>&1 || { echo "curl is required to download Gradle" >&2; exit 1; }
  command -v unzip >/dev/null 2>&1 || { echo "unzip is required to extract Gradle" >&2; exit 1; }
  mkdir -p "$GH/modforge"
  TMP="$GH/modforge/gradle-$VERSION.zip"
  echo "Downloading Gradle $VERSION..."
  curl -fL --retry 3 --connect-timeout 20 "$URL" -o "$TMP"
  unzip -q -o "$TMP" -d "$GH/modforge"
  rm -f "$TMP"
fi
exec "$DEST/bin/gradle" -p "$APP_HOME" "$@"
