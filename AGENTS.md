# Working in Artemis

FTC robot code maintained by high schoolers. Some are seeing a command scheduler for the first
time; some have three seasons on it and will notice if you're sloppy. Write for both.

**Tone:** a little personality is welcome — this is a codebase named after a moon goddess for a
team called Moonshots. One good joke per file, not per line, and never in place of an explanation.
Absurd variable names are fine when they're also the clearest name (`patience` for a timeout,
`breadcrumbs` for a pose trail, `megaphone` for telemetry). If a comment takes more than 15
seconds to read, cut it.

## Rule 1: don't break the merge

Artemis tracks `upstream` = `FTC-23511/SolversLib-Quickstart` and pulls it in every season. Every
file upstream can touch must stay byte-for-byte theirs, so that merge stays silent forever.

**Never hand-edit:** `README.md`, `build.gradle`, `build.common.gradle`,
`build.dependencies.gradle`, `gradle.properties`, `gradle/`, `gradlew*`, `FtcRobotController/`,
`.github/*`, `TeamCode/build.gradle`, `TeamCode/.../teamcode/pedroPathing/`,
`TeamCode/.../teamcode/samples/`.

**Always safe:** `docs/`, `MOONSHOTS.md`, `AGENTS.md`, `CLAUDE.md`, and our packages under
`TeamCode/.../teamcode/` — `utils/`, `subsystems/`, `commands/`, `opmodes/`, `MyRobot.java`.

Need something in the first list? You don't. Add a file beside it, or ask a human. This layout is
the entire point of the repo — don't undo it because a one-line edit looked easier.

Check yourself anytime: `./scripts/check-structure.sh`. It reports drift and changes nothing.
Run it before you commit, and after Android Studio offers you any kind of upgrade.

## Rule 2: telemetry has exactly one exit

`robot.sensors.addTelemetry(key, value)`. Never `telemetry.update()` anywhere but
`Sensors.periodic()`. Two flushes = half your data, no error message, one lost afternoon.

## Rule 3: `execute()` never blocks

No while-loops, no `Thread.sleep()` in a Command. The scheduler runs every active command's
`execute()` once per loop. Block one, freeze all. Wait by checking a timer in `isFinished()`.

## Where things go

```
MyRobot.java              subsystems, button bindings, autonomous plan
opmodes/                  DriveyMcDriverson (teleop), AutoMcAutty (auto)
subsystems/PedroDrive     mecanum + Pedro + dashboard drawing — tune it, don't rewrite it
subsystems/Sensors        the only telemetry flush
commands/DriveAbstract    base class for anything that moves the robot
utils/Constants           every hardware name and tunable number
```

Adding a mechanism? Copy `ExampleSubsystem` / `ExampleCommand`. Adding a movement? Extend
`DriveAbstract` — and give it a real timeout.

## Before you start

1. [docs/architecture.md](docs/architecture.md) — one page, explains the whole repo.
2. Changing drive behavior? The numbers are in `utils/Constants.java`, not scattered in code.
3. Build check: `./scripts/build.sh` (or `./gradlew :TeamCode:compileDebugJavaWithJavac`). Java-8
   deprecation warnings on a modern JDK are expected noise. Students deploy from Android Studio;
   `./scripts/deploy.sh` does the same thing from a terminal.
4. Lost an hour to something? Add it to [docs/issue-log.md](docs/issue-log.md) instead of fixing
   it silently.
