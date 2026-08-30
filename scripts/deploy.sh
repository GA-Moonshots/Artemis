#!/usr/bin/env bash
#
# deploy.sh — put the code on the robot from the terminal.
#
# Android Studio's Run button does the same two things (Gradle builds, adb
# installs). Use whichever is in front of you — this one is handy mid-terminal
# session, or when you want to see exactly what's happening.
#
#   ./scripts/deploy.sh           connect over WiFi, build, install
#   ./scripts/deploy.sh --usb     skip WiFi, use the USB cable
#
# WiFi: join the Control Hub's network first (the one named after your team).
# Override the address with:  ROBOT_HOST=1.2.3.4 ./scripts/deploy.sh

set -uo pipefail
source "$(dirname "$0")/_common.sh"
cd "$REPO"

ADB=$(require_adb) || exit 1
USE_USB=false
[ "${1:-}" = "--usb" ] && USE_USB=true

# ─── 1. Find the robot ───────────────────────────────────────────
if [ "$USE_USB" = false ]; then
    echo "${BOLD}Connecting to $ROBOT_HOST:$ROBOT_PORT...${OFF}"
    if ! "$ADB" connect "$ROBOT_HOST:$ROBOT_PORT" 2>&1 | grep -qE "connected to"; then
        echo "${RED}✗ Couldn't reach the Control Hub over WiFi.${OFF}"
        echo
        echo "Checklist:"
        echo "  • Is the robot powered on?"
        echo "  • Are you on the Control Hub's WiFi network (not the school's)?"
        echo "  • Try the cable instead: ${DIM}./scripts/deploy.sh --usb${OFF}"
        exit 1
    fi
fi

devices=$("$ADB" devices | tail -n +2 | grep -c "device$" || true)
if [ "$devices" -eq 0 ]; then
    echo "${RED}✗ No device connected.${OFF}"
    if [ "$USE_USB" = true ]; then
        echo "  Plug into the Control Hub's USB port and make sure it's powered on."
    fi
    exit 1
fi
if [ "$devices" -gt 1 ]; then
    echo "${YELLOW}! More than one device connected — Gradle may pick the wrong one.${OFF}"
    "$ADB" devices | tail -n +2 | sed 's/^/    /'
    echo "${DIM}    Unplug what you don't need, or: $ADB disconnect${OFF}"
    echo
fi

# ─── 2. Build and install ────────────────────────────────────────
echo "${BOLD}Building and installing...${OFF}"
echo

output=$(./gradlew :TeamCode:installDebug --console=plain 2>&1)
if [ $? -ne 0 ]; then
    echo "$output" | grep -E "^e: |error:|FAILURE:|What went wrong|^\s+> |Installation failed" | head -30
    echo
    echo "${RED}✗ Deploy failed.${OFF}"
    echo "${DIM}  Code didn't compile? Run ./scripts/build.sh for a cleaner error.${OFF}"
    echo "${DIM}  'INSTALL_FAILED_UPDATE_INCOMPATIBLE'? Uninstall first:${OFF}"
    echo "${DIM}    ./gradlew :TeamCode:uninstallDebug${OFF}"
    exit 1
fi

echo "${GREEN}✓ Installed.${OFF}"
echo
echo "On the Driver Station, pick your OpMode:"
echo "  ${BLUE}Drivey McDriverson${OFF}  ${DIM}teleop${OFF}"
echo "  ${BLUE}Auto McAutty${OFF}        ${DIM}autonomous${OFF}"
echo "  ${BLUE}Tuning${OFF}              ${DIM}drivetrain tuning — see docs/tuning.md${OFF}"
echo
echo "${DIM}Robot misbehaving? ./scripts/logs.sh${OFF}"
