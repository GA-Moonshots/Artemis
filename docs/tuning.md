# Tuning the drivetrain

Pedro Pathing 3 tunes from a **web page**, not a Driver Station OpMode. It's called AutoTune: each
tuner drives the robot (or asks you to push it), does the maths, and ends by printing a block of
Java for you to paste.

1. Deploy. Connect a laptop to the robot's Wi-Fi.
2. Open **<http://192.168.43.1:10158>**.
3. Run the tuners in the order below. Paste each result into
   [`utils/Constants.java`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/utils/Constants.java),
   then **redeploy** before starting the next one. Every tuner builds on the config above it.

The tuners are registered in [`utils/Tuning.java`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/utils/Tuning.java).
The procedures themselves (`MecanumTuner`, `PinpointTuner`, `ForesightTuner`, `Tests`) are copied
from Pedro's quickstart. Don't edit them: re-copy them when Pedro ships new ones.

Budget about an hour. Panels still runs alongside on port 8001.

## The sequence

| # | Tuner | Pastes into | Done when |
|---|---|---|---|
| 1 | Mecanum Tuner | `drivetrainConfig` | Every wheel spins the way the diagram says |
| 2 | Pinpoint Tuner | `localizerConfig` | The four hand tests below pass |
| 3 | Foresight Tuner | `foresightConfig` | It finishes without aborting |
| 4 | Tests | nothing | Hold, line, and curve all look right |

**When you paste, keep our names.** AutoTune prints hardware names as strings (`"leftFront"`).
Replace them with the `Constants` names (`LEFT_FRONT_NAME`) so a name is still typed in exactly
one place. For the same reason, paste the Foresight block over the controllers only and leave the
**end constraints** at the bottom alone. The tuner doesn't produce those; they're ours.

## After step 2: prove localization isn't lying

Run **Drivey McDriverson** and push the robot by hand while you watch telemetry:

| Test | Do this | Expect |
|---|---|---|
| Push | Push forward 12" | X goes up ~12" |
| Strafe | Push left 12" | Y goes up ~12" |
| Rotate | Turn 90° left by hand | Heading goes up ~90° |
| Spin | Spin a few circles, put it back | Position comes back to where it started |

Wrong direction? Flip `xPodDirection` / `yPodDirection` in `localizerConfig`, not the wiring. Drifts
while spinning? The pod offsets are off, so rerun the Pinpoint Tuner.

**Do not start step 3 until these pass.** Foresight tuned on bad localization is tuned against noise.

## What changed from Pedro 2

Pedro 2 had 17 separate tuners and a pile of PIDF coefficients: `translationalPIDF`, `drivePIDF`,
`centripetalScaling`, `mass`, and so on. None of those exist any more. Pedro 3's follower is
called **Foresight**. It measures how the robot coasts and brakes, then plans from that, so the
Foresight Tuner fills in roughly fifteen numbers you'd never want to guess by hand.

We carried over what still means the same thing: max velocities, natural deceleration (the old
zero-power accelerations), the pod offsets and directions, and the path end constraints. Everything
else in `foresightConfig` is marked `⚙ TUNE — NOT OUR ROBOT'S NUMBERS` until the Foresight Tuner runs.

## When it won't behave

- **Weaves or overshoots on straight paths** → rerun the Foresight Tuner on a fully charged battery.
- **Path never "finishes"** → loosen the end constraints at the bottom of `foresightConfig`.
- **Turns finish early or late** → `Tunables.HEADING_TOLERANCE_DEG`, live from Panels.
- **AutoTune page won't load** → are you on the robot's Wi-Fi? Is the app running?
- **Everything is bad** → go back to the hand tests. It's usually the hand tests.

Watch it live on the Panels dashboard. `PedroDrive` draws the robot and its breadcrumb trail every
loop, and a path that looks wrong on the field usually looks obviously wrong on the dashboard first.

Official reference: <https://pedropathing.com/docs/pathing/tuning>
