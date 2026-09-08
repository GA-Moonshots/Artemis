# When the robot won't localize

Symptoms that all mean the same thing: Panels coordinates frozen while the robot drives, the
Forward Zero Power tuner never starting its slowdown, autonomous driving as if it never moved.

All of those say **position data isn't arriving**. Work down this list in order — each step rules
out a whole layer, so don't skip ahead.

## 1. Is the sensor alive? — `Pinpoint Doctor`

Driver Station → **Diagnostics → Pinpoint Doctor**. It's a plain OpMode that talks to the Pinpoint
directly: no Pedro, no SolversLib, none of our code. That isolation is the point.

**Push the robot by hand and watch TICKS.**

| What you see | What it means |
|---|---|
| "NO PINPOINT FOUND" | Not in the robot config, or wired to nothing |
| "NAME MISMATCH" | It exists under a different name than `Constants.PINPOINT_NAME` |
| Status `FAULT_NO_PODS_DETECTED` | Neither pod cable is seated |
| Status `FAULT_X_POD_NOT_DETECTED` | Forward pod cable |
| Status `FAULT_BAD_READ` | I2C wiring, or an address clash on that bus |
| `READY` but ticks never change | Device is fine; the pods aren't physically turning |
| Both pods counting | **Sensor is fine — the fault is above it. Go to step 2.** |

If you get to "sensor is fine", stop blaming the wiring and start reading config.

## 2. Is Pedro configured to find it?

In [`utils/Constants.java`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/utils/Constants.java):

- **`PINPOINT_NAME`** must match the Driver Station config exactly, including case. Pedro defaults
  this internally to `"pinpoint"`; we set it explicitly so a mismatch is visible instead of silent.
- **`encoderResolution`** and the **pod offsets** — these match
  [Ganymede](https://github.com/GA-Moonshots/Ganymede) exactly, and Ganymede localized fine all
  last season on this hardware. So treat them as known-good until something else is ruled out.
  Compare against Ganymede whenever you're unsure what a working value looks like — same team,
  same robot parts, code that actually competed.

## 3. Do the directions and frame agree?

`Tuning → Localization → Localization Test`, then the four push tests in
[coordinates.md](coordinates.md): push forward 12" and confirm the reading moves 12" the right way.
If it moves the wrong way, flip `forwardEncoderDirection` / `strafeEncoderDirection` in `Constants`
— not the wiring.

## 4. Version skew

Ganymede ran SDK 11.1. We're on 11.2.1 with Pedro Pathing 2.0.6. If steps 1-3 all pass — sensor
READY, ticks moving, config matching a season that worked — then what changed is the SDK
underneath Pedro, not anything in this repo. Note it in [issue-log.md](issue-log.md) before you
start editing code.

## 5. Only then, suspect our code

`PedroDrive.periodic()` calls `follower.update()` exactly once per loop, and that's the only place
it should ever be called. If the numbers in the *telemetry* move but the *drawing* doesn't, that's
a drawing problem, not a localization one — check the frame markers first
([coordinates.md](coordinates.md)).

Watch **Loop (ms)** on the dashboard while you debug. Pedro integrates position over time, so a
loop that has ballooned to 50ms+ degrades position quality on its own.

## The habit worth keeping

Each step above isolates one layer. The reason "the flow of information seems erroneous" is so hard
to chase is that four layers can produce identical symptoms — so test them one at a time, from the
sensor outward, and write down which one it turned out to be in
[issue-log.md](issue-log.md).
