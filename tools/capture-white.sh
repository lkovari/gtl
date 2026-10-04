#!/bin/bash
# Run while a broken screen (e.g. white screen) is visible on the connected phone.
# Saves the view tree with sizes, gfxinfo, logcat, the app error log, and a screenshot.
# Usage: tools/capture-white.sh [output-dir]   (default: captures/white-HHMMSS, gitignored)

set -u

PACKAGE=com.lkovari.mobile.apps.gtl
ROOT=$(cd "$(dirname "$0")/.." && pwd)

if [ -n "${ANDROID_HOME:-}" ] && [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
    ADB="$ANDROID_HOME/platform-tools/adb"
elif [ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
elif command -v adb >/dev/null 2>&1; then
    ADB=adb
else
    echo "adb not found. Set ANDROID_HOME or put adb on PATH." >&2
    exit 1
fi

if [ "$("$ADB" get-state 2>/dev/null)" != "device" ]; then
    echo "No phone connected (check USB debugging)." >&2
    exit 1
fi

OUT=${1:-"$ROOT/captures/white-$(date +%H%M%S)"}
mkdir -p "$OUT"

"$ADB" shell dumpsys activity top > "$OUT/activity-top.txt"
"$ADB" shell dumpsys gfxinfo "$PACKAGE" > "$OUT/gfxinfo.txt"
"$ADB" logcat -d -v threadtime -b main,system,crash,events > "$OUT/logcat.txt"
"$ADB" shell run-as "$PACKAGE" cat files/diagnostics/errors.log > "$OUT/errors.log" 2>&1
"$ADB" exec-out screencap -p > "$OUT/screen.png"

echo "saved to $OUT"
