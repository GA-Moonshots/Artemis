# Issue log

Dated gotchas. Add one when you lose an hour to something — that's the whole point.

Format: `## YYYY-MM-DD — title`, then what broke, why, and the fix.

---

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
