#!/usr/bin/env bash
#
# Capture the Play Store screenshot set from a debug build.
#
#   tools/screenshots.sh [light|dark]
#
# Requires a booted emulator or device on adb. Builds and installs the debug
# APK, seeds demo data, then walks the app and captures each screen.
#
# Why the ordering below is what it is:
#
#   1. `pm clear` then launch, so the app creates a fresh database.
#   2. Seed once. This is what marks onboarding complete — but the onboarding
#      screen is already composed by now, so it stays on screen.
#   3. Force-stop and relaunch. Onboarding is skipped this time.
#   4. Seed again. CompanionViewModel.startFresh parks the whole conversation on
#      every launch by design, so the only way to have a conversation on screen
#      is to insert messages after the launch that parked it. Ids climb across
#      the wipe (AUTOINCREMENT), so the new messages land above the watermark.
#
# Navigation goes through the axiom:// deep links wherever one exists, because
# tap coordinates break the moment anything moves. Screens without a deep link
# are reached by tapping, and those taps are the fragile part of this script.

set -euo pipefail

MODE="${1:-light}"
PKG="com.cosmiclaboratory.axiom"
ACTIVITY="$PKG/.MainActivity"
RECEIVER="$PKG/.demo.DemoSeedReceiver"
SEED_ACTION="com.cosmiclaboratory.axiom.DEMO_SEED"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/screenshots/$MODE"

mkdir -p "$OUT"

shot() {
    sleep "${2:-3}"
    adb exec-out screencap -p > "$OUT/$1.png"
    echo "  captured $1"
}

deeplink() {
    adb shell am start -a android.intent.action.VIEW -d "$1" "$PKG" > /dev/null 2>&1
}

seed() {
    adb shell am broadcast -n "$RECEIVER" -a "$SEED_ACTION" > /dev/null 2>&1
    sleep 8
}

echo "==> Building and installing debug build"
"$ROOT/gradlew" -p "$ROOT" :app:assembleDebug -q
adb install -r -t "$ROOT/app/build/outputs/apk/debug/app-debug.apk" > /dev/null

echo "==> Setting up device"
adb shell settings put system show_touches 0 > /dev/null 2>&1 || true
# A clean status bar: full signal, full battery, no clutter, fixed clock.
adb shell cmd statusbar overlay-icon-visibility > /dev/null 2>&1 || true
adb emu avd name > /dev/null 2>&1 || true

case "$MODE" in
    dark)  adb shell cmd uimode night yes > /dev/null 2>&1 ;;
    light) adb shell cmd uimode night no  > /dev/null 2>&1 ;;
esac

echo "==> Seeding (pass 1: clears onboarding)"
adb shell pm clear "$PKG" > /dev/null
adb shell am start -n "$ACTIVITY" > /dev/null 2>&1
sleep 7
seed

echo "==> Relaunching past onboarding, seeding again for a live conversation"
adb shell am force-stop "$PKG"
sleep 1
adb shell am start -n "$ACTIVITY" > /dev/null 2>&1
sleep 7
seed

echo "==> Capturing"
shot "01-companion" 3

deeplink "axiom://journal"
shot "02-journal" 3

deeplink "axiom://patterns"
shot "03-patterns" 4
# "the numbers, if you'd like them" is collapsed by default, and the charts
# behind it are the most screenshot-worthy thing on the screen. The y here is
# the only hard-coded coordinate in this script; if the findings list changes
# length it will need moving.
adb shell input tap 400 1520
shot "03b-patterns-numbers" 3

deeplink "axiom://settings/ai"
shot "04-settings-ai" 3

deeplink "axiom://composer"
shot "05-composer" 3

echo
echo "Captured into $OUT"
echo
echo "Still to do by hand — these have no deep link, so navigate and run:"
echo "    adb exec-out screencap -p > $OUT/06-memory.png     # menu -> What I remember"
echo "    adb exec-out screencap -p > $OUT/07-search.png     # journal -> search"
echo "    adb exec-out screencap -p > $OUT/08-calendar.png   # journal -> calendar"
echo "    adb exec-out screencap -p > $OUT/09-reader.png     # tap any entry"
echo "    adb exec-out screencap -p > $OUT/10-talks.png      # 'Our talks'"
