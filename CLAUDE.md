# Artemis — agent rules

@AGENTS.md

The import above pulls in the full guidance. If your tool doesn't support `@` imports, open
`AGENTS.md` yourself before doing anything. The three rules that matter most, restated here so
they're in context no matter what:

## 1. Never edit files upstream owns

This repo merges `upstream/master` every season. These stay byte-for-byte upstream's:

`README.md` · `build.gradle` · `build.common.gradle` · `build.dependencies.gradle` ·
`gradle.properties` · `settings.gradle` · `gradlew*` · `gradle/` · `FtcRobotController/` ·
`.github/` · `TeamCode/build.gradle` · `TeamCode/.../teamcode/pedroPathing/` ·
`TeamCode/.../teamcode/samples/`

Our layer — safe to edit, invisible to upstream: `docs/`, `scripts/`, `MOONSHOTS.md`, `AGENTS.md`,
`CLAUDE.md`, and `TeamCode/.../teamcode/{utils,subsystems,commands,opmodes}/` + `MyRobot.java`.

Think you need to edit a protected file? You don't. Add a file beside it, or ask a human.
Verify anytime with `./scripts/check-structure.sh`.

## 2. Telemetry has exactly one exit

`robot.sensors.addTelemetry(key, value)`. Never call `telemetry.update()` outside
`Sensors.periodic()`. Two flushes = half your data, no error, one lost afternoon.

## 3. `execute()` never blocks

No while-loops, no `Thread.sleep()` in a Command. The scheduler runs every active command's
`execute()` once per loop — block one, freeze all. Wait by checking a timer in `isFinished()`.

---

Structure, tone, and the rest: `AGENTS.md`. Architecture: `docs/architecture.md`.
