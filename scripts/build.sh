#!/usr/bin/env bash
#
# build.sh — does the code compile?
#
# The fast inner loop. Run this constantly; it's the same check Android Studio
# runs, minus the IDE. No robot required.
#
#   ./scripts/build.sh          compile TeamCode
#   ./scripts/build.sh --apk    also package the APK

set -uo pipefail
source "$(dirname "$0")/_common.sh"
cd "$REPO"

TASK=":TeamCode:compileDebugJavaWithJavac"
LABEL="Compiling"
if [ "${1:-}" = "--apk" ]; then
    TASK=":TeamCode:assembleDebug"
    LABEL="Building APK"
fi

echo "${BOLD}$LABEL...${OFF}"
echo

output=$(./gradlew "$TASK" --console=plain 2>&1)
status=$?

# Real problems only. The Java-8-on-a-modern-JDK deprecation warnings are
# expected noise — see docs/gradle-and-android-studio.md.
echo "$output" | grep -E "^e: |error:|FAILURE:|What went wrong|^\s+> " | head -30

if [ $status -eq 0 ]; then
    echo "${GREEN}✓ Compiles clean.${OFF}"
    if [ "${1:-}" = "--apk" ]; then
        apk=$(find TeamCode/build/outputs/apk -name "*.apk" -newermt "-2 minutes" 2>/dev/null | head -1)
        [ -n "$apk" ] && echo "${DIM}  APK: $apk${OFF}"
    fi
    echo "${DIM}  Push it to the robot with ./scripts/deploy.sh${OFF}"
    exit 0
fi

echo
echo "${RED}✗ Build failed.${OFF}"
echo "${DIM}  Full output: ./gradlew $TASK${OFF}"
echo "${DIM}  Stuck on a Gradle/Android Studio complaint? docs/gradle-and-android-studio.md${OFF}"
exit 1
