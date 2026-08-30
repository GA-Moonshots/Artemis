#!/usr/bin/env bash
#
# logs.sh — what is the robot actually saying?
#
# When an OpMode crashes, the Driver Station shows a short message and the
# real stack trace goes to the robot's log. This is how you read it.
#
#   ./scripts/logs.sh           live robot log (Ctrl-C to stop)
#   ./scripts/logs.sh --crash   only crashes and errors
#   ./scripts/logs.sh --clear   wipe the log, then follow (run before a test)

set -uo pipefail
source "$(dirname "$0")/_common.sh"

ADB=$(require_adb) || exit 1

if [ "$("$ADB" devices | tail -n +2 | grep -c "device$" || true)" -eq 0 ]; then
    echo "${RED}✗ No robot connected.${OFF}"
    echo "${DIM}  Connect first: ./scripts/deploy.sh  (or --usb)${OFF}"
    exit 1
fi

case "${1:-}" in
    --clear)
        "$ADB" logcat -c
        echo "${GREEN}✓ Log cleared.${OFF} Following — Ctrl-C to stop."
        echo
        exec "$ADB" logcat -v brief RobotCore:V System.err:V AndroidRuntime:E "*:S"
        ;;
    --crash)
        echo "${BOLD}Errors and crashes only.${OFF} Ctrl-C to stop."
        echo "${DIM}Look for 'Caused by:' — that line is usually the real problem.${OFF}"
        echo
        exec "$ADB" logcat -v brief AndroidRuntime:E RobotCore:E System.err:W "*:S"
        ;;
    *)
        echo "${BOLD}Robot log.${OFF} Ctrl-C to stop."
        echo "${DIM}Only crashes? ./scripts/logs.sh --crash${OFF}"
        echo
        exec "$ADB" logcat -v brief RobotCore:V System.err:V AndroidRuntime:E "*:S"
        ;;
esac
