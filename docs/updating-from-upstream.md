# Pulling the new season's SDK

```bash
git fetch upstream
git merge upstream/master
```

`upstream` is `FTC-23511/SolversLib-Quickstart`, which itself tracks FIRST's `FtcRobotController`.

## What should happen: nothing dramatic

`README.md`, the `build*.gradle` files, `gradle/`, `FtcRobotController/`, and TeamCode's
`pedroPathing/` and `samples/` are all upstream's. We've never hand-edited them, so they merge
clean. Our layer — `docs/`, `MOONSHOTS.md`, `AGENTS.md`, `CLAUDE.md`, and the `utils/`,
`subsystems/`, `commands/`, `opmodes/` packages — is invisible to upstream and untouched.

## Where a conflict is genuinely expected

**`TeamCode/build.gradle`** — ships from upstream *and* is the file FTC's own convention says teams
should customize. We haven't needed to (SolversLib, Pedro, Dashboard, and Panels are already in
it), but if we ever add a dependency, expect to merge this one by hand.

A conflict anywhere else means something got edited that shouldn't have been. Check the never-edit
list in [AGENTS.md](../AGENTS.md), and fix the layout rather than just the conflict.

## After merging

1. `./gradlew :TeamCode:compileDebugJavaWithJavac` — catch API changes immediately.
2. Re-check [gradle-and-android-studio.md](gradle-and-android-studio.md); Gradle, AGP, and
   compileSdk all move with upstream, and Android Studio's requirements move independently.
3. SolversLib or Pedro major version bump? Skim their changelogs — `PedroDrive` and `DriveAbstract`
   are where breakage would land.
4. Anything surprising goes in [issue-log.md](issue-log.md).
