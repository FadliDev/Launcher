#!/usr/bin/env sh
# Small bootstrap wrapper for environments where the binary Gradle Wrapper JAR is unavailable.
# Downloads the official, version-pinned Gradle distribution and verifies SHA-256 before execution.
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION="8.11.1"
EXPECTED_SHA256="f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6"
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"${HOME:-$APP_HOME}/.gradle"}
CACHE_DIR="$GRADLE_USER_HOME/sally-wrapper"
DIST_DIR="$CACHE_DIR/gradle-$VERSION"
GRADLE_BIN="$DIST_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$CACHE_DIR"
    ZIP="$CACHE_DIR/gradle-$VERSION-bin.zip"
    TMP="$ZIP.part"
    echo "Gradle $VERSION belum tersedia; mengunduh distribusi resmi..."
    rm -f "$TMP"
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --retry 3 --connect-timeout 20 \
            "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$TMP"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$TMP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
    else
        echo "Perlu curl atau wget untuk mengunduh Gradle." >&2
        exit 1
    fi
    if command -v sha256sum >/dev/null 2>&1; then
        ACTUAL=$(sha256sum "$TMP" | awk '{print $1}')
    elif command -v shasum >/dev/null 2>&1; then
        ACTUAL=$(shasum -a 256 "$TMP" | awk '{print $1}')
    else
        echo "Perlu sha256sum atau shasum untuk memverifikasi Gradle." >&2
        rm -f "$TMP"
        exit 1
    fi
    if [ "$ACTUAL" != "$EXPECTED_SHA256" ]; then
        echo "Checksum Gradle tidak cocok; file unduhan ditolak." >&2
        rm -f "$TMP"
        exit 1
    fi
    mv "$TMP" "$ZIP"
    rm -rf "$DIST_DIR" "$CACHE_DIR/gradle-$VERSION"
    if ! command -v unzip >/dev/null 2>&1; then
        echo "Perlu unzip untuk mengekstrak Gradle." >&2
        exit 1
    fi
    unzip -q "$ZIP" -d "$CACHE_DIR"
fi

exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
