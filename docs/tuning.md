# Tuning the drivetrain

Everything here happens in one OpMode: **"Tuning"** (group: Pedro Pathing) on the Driver Station.
It opens a menu. The folders below match that menu exactly.

Budget 60–90 minutes. Do it in order — each phase assumes the ones above it are done, and tuning
PIDs before velocities means tuning them twice.

All values you produce go into [`utils/Constants.java`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/utils/Constants.java).

## Step 0: prove localization isn't lying

`Tuning → Localization → Localization Test`. Push the robot by hand:

| Test | Do this | Expect |
|---|---|---|
| Push | Push forward 12" | Telemetry says ~12" |
| Rotate | Turn 90° by hand | Heading changes ~π/2 (1.57) |
| Strafe | Push sideways | X and Y change the way you'd expect |
| Spin | Spin in circles | Heading wraps at ±π, no jumps |

Wrong direction? Fix `forwardEncoderDirection` / `strafeEncoderDirection` in Constants — not the
wiring. Wrong distance? Check `forwardPodY` / `strafePodX`, measured from robot center.

**Do not skip this.** Tuning PIDs on bad localization is tuning against noise.

## The 8 phases

| # | Menu path | Sets | Done when |
|---|---|---|---|
| 1 | Automatic → Forward Zero Power Acceleration Tuner | `forwardZeroPowerAcceleration` | Stops clean, no slide |
| 2 | Automatic → Lateral Zero Power Acceleration Tuner | `lateralZeroPowerAcceleration` | Stops clean strafing |
| 3 | Automatic → Forward Velocity Tuner | `xVelocity` | Reported ≈ real max speed |
| 4 | Automatic → Lateral Velocity Tuner | `yVelocity` | Reported ≈ real strafe speed |
| 5 | Manual → Translational Tuner | `translationalPIDFCoefficients` | Shoved sideways, returns smoothly |
| 6 | Manual → Heading Tuner | `headingPIDFCoefficients` | Twisted, snaps back once |
| 7 | Manual → Drive Tuner | `drivePIDFCoefficients` | Follows a path without weaving |
| 8 | Manual → Centripetal Tuner | `centripetalScaling` | Corners neither cut nor swing wide |

Phases 5–8 are PIDs: **start with P alone.** Raise P until it corrects briskly, then back off until
it stops oscillating. Add D only if it still overshoots. I is almost never the answer.

Weigh the robot (battery in) and put the kg value in `.mass()` before you start — it's the one
number you measure instead of tune.

## Prove it worked

`Tuning → Tests → Line`, then `Triangle`, then `Circle`. If Circle looks like a circle, you're done.
Watch it live on the Panels dashboard — `PedroDrive` draws the robot and its breadcrumb trail every
loop, and a path that looks wrong on the field usually looks obviously wrong on the dashboard first.

## When it won't behave

- **Weaving down straight paths** → phase 7, lower P.
- **Cutting corners** → phase 8, raise `centripetalScaling`.
- **Swinging wide on corners** → phase 8, lower it.
- **Robot hunts back and forth on heading** → phase 6, lower P, consider a little D.
- **Everything is bad** → go back to Step 0. It's usually Step 0.

Official reference: <https://pedropathing.com/docs/pathing/tuning>
