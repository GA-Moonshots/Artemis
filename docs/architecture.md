# How this code is put together

Instead of one giant OpMode full of nested ifs, you write small classes that each do one thing.
A scheduler runs them all, every loop, without any of them blocking the others.

Four ideas, and then you can read the whole repo:

**Subsystem** — one mechanism. Owns its motors and servos. `PedroDrive` is the drivetrain.

**Command** — one action, over time. Four methods:

```java
initialize()          // once, when scheduled     — start a timer, kick off a path
execute()             // every loop while active  — nudge, check, report
isFinished()          // every loop, after execute — true when done
end(interrupted)      // once, on finish OR cancel — stop motors, clean up
```

**`execute()` must never block.** No while-loops, no `Thread.sleep()`. The scheduler calls every
active command's `execute()` once per loop — block in one and the whole robot freezes. To wait,
check a timer in `isFinished()`.

**MyRobot** — owns every subsystem, holds the button bindings, holds the autonomous plan.

**OpMode** — builds a MyRobot and gets out of the way. `DriveyMcDriverson` (teleop),
`AutoMcAutty` (autonomous).

## Default commands

`drive.setDefaultCommand(new Drive(this))` means: run `Drive` whenever nothing else has claimed the
wheels. Schedule `DriveToPose` and it interrupts `Drive`; when it finishes, `Drive` resumes on its
own. That's why the driver never has to press anything to get control back.

Default commands return `false` from `isFinished()` forever. They don't end — they get interrupted.

## The drive command family

`DriveAbstract` is the base class for anything that moves the robot. It hands subclasses
`robot`/`drive`/`follower`/`patience` pre-wired, claims the drive subsystem, and provides
`standardCleanup()`.

```
DriveAbstract  (base: references, timeout, cleanup)
├── DriveToPose      go stand exactly there
├── DriveFwdByDist   go that way N inches
├── DriveTurnBy      rotate N degrees (relative)
└── DriveTurnTo      face this heading (absolute)
```

Adding a fifth is the normal way to extend this — copy whichever is closest.

**Every one takes a timeout, and that is not optional.** A command that never quite reaches its
tolerance otherwise runs until the match ends, blocking everything queued behind it. The timer is
called `patience`; when it runs out, the command ends whether or not it arrived.

That's also why the turn commands are ours rather than SolversLib's. Theirs are
`isFinished() { return !follower.isBusy(); }` — no timeout, no cleanup. Pin the robot against a
wall and it never finishes.

Note `DriveToPose.execute()` is nearly empty. Commands hand the follower a path in `initialize()`;
the follower does the actual driving from `PedroDrive.periodic()`. Commands say *what*, the
follower handles *how*.

## Two rules that will bite you

**All telemetry goes through `Sensors`.** Call `robot.sensors.addTelemetry(...)`. Never call
`telemetry.update()` anywhere else — `Sensors.periodic()` is the only flush in the project, and a
second one costs you half your data. `Sensors` also owns the Limelight and the AprilTag trust
policy, because a camera is a sensor.

**`follower.update()` runs exactly once per loop**, in `PedroDrive.periodic()`. Zero times and the
robot thinks it never moved; twice and it thinks it moved twice as far.

## Where things are

Three folders under `teamcode/`, and that's the whole structure:

```
MyRobot.java                 subsystems, button bindings, auto plan

commands/Drive               default teleop drive
commands/DriveAbstract       base for all movement commands
commands/DriveToPose         ├ go stand exactly there
commands/DriveFwdByDist      ├ go that way N inches
commands/DriveTurnBy         ├ rotate N degrees
commands/DriveTurnTo         └ face this heading

subsystems/PedroDrive        mecanum + Pedro localization
subsystems/Sensors           shared sensors, the Limelight, the only telemetry flush

utils/Constants              hardware names, directions, follower config (final)
utils/Tunables               live-editable from the dashboard (not final)
utils/FieldView              all dashboard drawing, incl. frame markers
utils/FieldMap               tag positions + coordinate conversions
utils/PersistentPoseManager  auto → teleop pose handoff
utils/DriveyMcDriverson      teleop entry point
utils/AutoMcAutty            autonomous entry point
utils/CameraCalibration      is the camera telling the truth?
utils/Tuning                 the 17 drivetrain tuners
```

OpModes live in `utils/`. Don't add a fourth folder.

`ExampleSubsystem` and `ExampleCommand` are templates. Copy them for this year's mechanisms, then
delete them.

Coordinates and the frame of reference: [coordinates.md](coordinates.md) — read it before writing
a path. Tuning the drivetrain: [tuning.md](tuning.md). Deeper theory the code doesn't duplicate:
the team [GitBook](https://gilmour.online/compsci/competitive-robotics/software-team).

SolversLib supplies `Robot`, `CommandOpMode`, `SubsystemBase`, and `CommandBase` — it's a Gradle
dependency, not code we own. Pedro Pathing supplies the follower. Neither is forked; both update by
version bump ([updating-from-upstream.md](updating-from-upstream.md)).
