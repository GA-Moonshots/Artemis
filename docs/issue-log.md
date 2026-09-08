# Issue log

Dated gotchas. Add one when you lose an hour to something — that's the whole point.

Format: `## YYYY-MM-DD — title`, then what broke, why, and the fix.

---

## 2026-09-08 — Localization dead: Panels frozen, tuner never slows down

Two symptoms, one cause. The Forward Zero Power tuner builds its OWN follower from
`Constants.createFollower()` and touches none of our subsystem code — so the fact that it failed
*too* ruled out `PedroDrive`, `Drive`, and the drawing code immediately, and pointed at localization.

Ruled out by reading the libraries, not guessing:
- **Bulk caching is NOT the culprit.** `MANUAL` mode freezes sensors if nobody clears the cache,
  but SolversLib's `CommandScheduler.run()` calls `clearBulkCache()` at the end of every loop when
  the mode is MANUAL. Verified in its source.
- **`PINPOINT_NAME` was an invisible default.** Pedro defaults `hardwareMapName` to `"pinpoint"`
  internally and we never set it, so a config named anything else would fail silently. Now set
  explicitly in `Constants`.

Added `Pinpoint Doctor` (Diagnostics group): a plain LinearOpMode that reads the device directly —
no Pedro, no SolversLib, no MyRobot — reporting device id, `DeviceStatus`, update rate, and raw
encoder ticks. Push the robot; if ticks move, the sensor is fine and the fault is above it.
Playbook: [diagnostics.md](diagnostics.md).

**Retracted a bad lead.** I flagged `encoderResolution` (`goBILDA_SWINGARM_POD`) as suspect.
Checked Ganymede on GitHub: its Pinpoint block is byte-for-byte identical to ours — same pods,
offsets, and directions — and it worked all last season. So the pod type is right, and config is
not what changed. Ganymede is a useful control whenever localization misbehaves: same team, same
hardware, known-good numbers.

**What did change:** Ganymede ran SDK 11.1; Artemis is on 11.2.1 with Pedro 2.0.6. If the Doctor
reports READY with ticks moving — sensor and config both fine — that version seam is the next
place to look, not our subsystem code.

**Also fixed today, unrelated but noisy:** `check-structure.sh` reported 13 files as drift. All
false. The `upstream` remote had reverted to the SolversLib Quickstart, so the script was comparing
our FIRST-based tree against the wrong repo. Remote repointed at FIRST, and the script now refuses
to run that check at all when `upstream` isn't FIRST's SDK — a checker that cries wolf is worse
than no checker.

## 2026-08-29 — Migrated upstream: Quickstart → FIRST's SDK

The SolversLib Quickstart hadn't been touched since February and held us at SDK v11.1. Repointed
`upstream` to `FIRST-Tech-Challenge/FtcRobotController` (shared git ancestry, so it merged
normally) and jumped to **v11.2.1 / Gradle 9.1 / AGP 8.13.2**. FIRST's `TeamCode` contains only a
readme, which is why `pedroPathing/` and `samples/` are gone — those were Quickstart injections.
`Tuning.java` was rescued into `utils/` before deleting the rest. SolversLib is a *dependency*, not
an upstream; see [updating-from-upstream.md](updating-from-upstream.md).

## 2026-08-29 — SolversLib 0.3.4 hard-pins the SDK version

After moving to SDK 11.2.1 the build died with:

```
Cannot find a version of 'org.firstinspires.ftc:FtcCommon' that satisfies the version constraints:
  org.solverslib:core:0.3.4 --> org.firstinspires.ftc:FtcCommon:{strictly 11.1.0}
```

`{strictly}` can't be overridden by normal resolution. **SolversLib 0.3.5 dropped the pin
entirely** (its POM lists only kotlin-stdlib, ejml, androidx.core), so the fix was bumping to
0.3.5. Lesson: when an SDK bump fails on a version constraint, check whether the *library* pins it
before touching anything in the SDK.

## 2026-08-29 — FIRST's stock compileSdk doesn't build

FIRST v11.2.1 ships `compileSdkVersion 30` in `build.common.gradle` and
`FtcRobotController/build.gradle`. With AGP 8.13.2 and our dependency set that fails outright
("Recommended action: Update this project to use a newer compileSdk"). We keep `compileSdk 34` as
a deliberate deviation — it's in `ALLOWED_DRIFT` in `scripts/check-structure.sh`, so the structure
check reports it as expected rather than as an error. If a future merge reverts it to 30, put 34
back.

## 2026-08-29 — Android Studio silently edited settings.gradle

Found `settings.gradle` modified with a `foojay-resolver-convention` plugin block nobody added on
purpose — Android Studio wrote it during a Gradle sync. Exactly the scenario
`check-structure.sh` exists for, and it caught it. Discarded; FIRST's v11.2.1 `settings.gradle`
supplies its own `pluginManagement` block that covers the same ground.

## 2026-08-29 — Upstream's `pedroPathing/Constants.java` can't actually drive

The stub upstream ships builds a follower with no drivetrain and no localizer:

```java
return new FollowerBuilder(followerConstants, hardwareMap)
        .pathConstraints(pathConstraints)
        .build();          // no .mecanumDrivetrain(), no .pinpointLocalizer()
```

Our real config lives in [`utils/Constants.java`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/utils/Constants.java)
instead — which also keeps it in our layer, so upstream merges never touch it. Use
`utils.Constants.createFollower()`. The upstream file stays untouched; the `Tuning` OpMode uses it.

## 2026-08-29 — GitBook says we wrote `Robot.java` / `CommandOpMode`; we didn't

The "Under the Hood" page presents both as living in "our utils folder." Diffed against SolversLib
0.3.4 sources: byte-for-byte `com.seattlesolvers.solverslib.command.Robot` and `.CommandOpMode`.
We extend the library classes directly — no shadow copies of code we don't own. Worth correcting
on the GitBook.

## 2026-08-29 — Build verified clean

`./gradlew :TeamCode:compileDebugJavaWithJavac --rerun-tasks` → `BUILD SUCCESSFUL`, warnings only
(Java 8 deprecation on JDK 25, non-incremental `OpModeAnnotationProcessor`). See
[gradle-and-android-studio.md](gradle-and-android-studio.md). Pinned: Gradle 8.9, AGP 8.7.0,
compileSdk 34.

## Template

```
## YYYY-MM-DD — title

What broke, what you were doing, the actual root cause, and the fix. "Reinstalled Android Studio"
is not a root cause.
```
