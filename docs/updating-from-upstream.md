# Updates: two kinds, two mechanisms

Artemis pulls from two directions, and they work completely differently. Knowing which is which
saves you from merging something that was never meant to be merged.

## 1. The FTC SDK — a merge, once a season

`upstream` is **`FIRST-Tech-Challenge/FtcRobotController`**: the SDK itself, straight from FIRST.

```bash
git fetch upstream
git merge upstream/master
```

FIRST's `TeamCode` holds only a `readme.md`, so their releases barely touch our code. Expect
changes in `FtcRobotController/`, the root `build*.gradle` files, and `gradle/`.

## 2. Libraries — a version bump, any time

SolversLib, Pedro Pathing, Panels, and FTC Dashboard are **dependencies, not forks.** They live as
version numbers in `TeamCode/build.gradle`:

```gradle
implementation "org.solverslib:core:0.3.5"
implementation "org.solverslib:pedroPathing:0.3.5"
implementation 'com.pedropathing:ftc:2.0.6'
implementation "com.bylazar:fullpanels:1.0.12"
```

Change the number, rebuild, done. No merge, no conflict, nothing to resolve. Latest versions:
[SolversLib releases](https://github.com/FTC-23511/SolversLib/releases) ·
[Pedro Pathing](https://pedropathing.com).

Bump one at a time and build in between. When something breaks you want to know which one did it.

## Known deviations from upstream

Two files carry a deliberate edit, and `check-structure.sh` reports them as expected rather than
as errors:

| File | Why |
|---|---|
| `build.common.gradle` | `compileSdk 34` — FIRST ships 30, which **fails to build** with our dependencies |
| `FtcRobotController/build.gradle` | same |

If a merge reverts those to 30, the build breaks immediately with "Recommended action: Update this
project to use a newer compileSdk." Put 34 back.

## After any update

1. `./scripts/build.sh` — catch API changes immediately.
2. `./scripts/check-structure.sh` — confirm nothing drifted beyond the two known deviations.
3. Surprises go in [issue-log.md](issue-log.md).

## Why not fork SolversLib?

It's a library — `core`, `pedroPathing`, `photon` modules published to Maven. It contains no
Android app, so nothing in it installs on a Control Hub. Forking it to get a robot project is like
forking React to get a website. Fork it only if you intend to modify the library itself; consume it
as a dependency otherwise.
