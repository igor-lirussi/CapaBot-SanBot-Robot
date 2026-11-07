#!/usr/bin/env bash
# Build CapaBot 6.0.1-googletts with local Google TTS + Settings toggle.
# Requires JDK 11+ compatible with this project's Gradle wrapper.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/Users/tim.brien/Developer/jdks/jdk-11.0.32.1+1/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"
APKSIGNER="${APKSIGNER:-$HOME/Library/Android/sdk/build-tools/35.0.0/apksigner}"
ZIPALIGN="${ZIPALIGN:-$HOME/Library/Android/sdk/build-tools/35.0.0/zipalign}"
DEBUG_KS="${DEBUG_KS:-$HOME/.android/debug.keystore}"

cd "$ROOT"
./gradlew assembleDebug --no-daemon

BUILT="$ROOT/build/outputs/apk/debug/CapaBot-SanBot-Robot-debug.apk"
WORKDIR="$(mktemp -d)"
cp "$BUILT" "$WORKDIR/app.apk"
mkdir -p "$WORKDIR/lib/armeabi-v7a" "$WORKDIR/lib/armeabi"
cp "$ROOT/src/main/jniLibs/armeabi-v7a/libUvcNative.so" "$WORKDIR/lib/armeabi-v7a/"
cp "$ROOT/src/main/jniLibs/armeabi/libUvcNative.so" "$WORKDIR/lib/armeabi/"
cd "$WORKDIR"
zip -d app.apk 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.MF' 'META-INF/*.DSA' >/dev/null 2>&1 || true
zip -u app.apk lib/armeabi-v7a/libUvcNative.so lib/armeabi/libUvcNative.so >/dev/null
"$ZIPALIGN" -f 4 app.apk app-aligned.apk
"$APKSIGNER" sign --ks "$DEBUG_KS" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey app-aligned.apk
"$APKSIGNER" verify app-aligned.apk

OUT="$HOME/sanbot/apk/CapaBot-SanBot-Robot-6.0.5-googletts-debug.apk"
cp app-aligned.apk "$BUILT"
cp app-aligned.apk "$OUT"
echo "Built: $OUT"
echo "Install: adb install -r -d \"$OUT\""
