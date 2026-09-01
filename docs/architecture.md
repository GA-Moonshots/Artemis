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
└── DriveFwdByDist   go that way N inches
```

Adding a third is the normal way to extend this — copy whichever is closer.

Every one takes a timeout. **This is not optional**: a path command that never quite reaches
tolerance will otherwise run until the match ends, blocking everything queued behind it. That's
what the field is named `patience` — when it runs out, the command ends whether or not it arrived.

Note `DriveToPose.execute()` is nearly empty. Commands hand the follower a path in `initialize()`;
the follower does the actual driving from `PedroDrive.periodic()`. Commands say *what*, the
follower handles *how*.

## Two rules that will bite you

**All telemetry goes through `Sensors`.** Call `robot.sensors.addTelemetry(...)`. Never call
`telemetry.update()` anywhere else — `Sensors.periodic()` is the only flush in the project, and a
second one costs you half your data.

**`follower.update()` runs exactly once per loop**, in `PedroDrive.periodic()`. Zero times and the
robot thinks it never moved; twice and it thinks it moved twice as far.

## Where things are

Three folders under `teamcode/`, and that's the whole structure:

```
MyRobot.java               subsystems, button bindings, auto plan

commands/Drive             default teleop drive
commands/DriveAbstract     base for all movement commands
commands/DriveToPose       ├ go stand exactly there
commands/DriveFwdByDist    └ go that way N inches

subsystems/PedroDrive      mecanum + Pedro + dashboard drawing
subsystems/Sensors         the only telemetry flush

utils/Constants            every hardware name and tunable number
utils/DriveyMcDriverson    teleop entry point
utils/AutoMcAutty          autonomous entry point
utils/Tuning               the 17 drivetrain tuners
```

OpModes live in `utils/`. Don't add a fourth folder.

`ExampleSubsystem` and `ExampleCommand` are templates. Copy them for this year's mechanisms, then
delete them.

Tuning the drivetrain: [tuning.md](tuning.md). Deeper theory the code doesn't duplicate:
the team [GitBook](https://gilmour.online/compsci/competitive-robotics/software-team).

SolversLib supplies `Robot`, `CommandOpMode`, `SubsystemBase`, and `CommandBase` — it's a Gradle
dependency, not code we own. Pedro Pathing supplies the follower. Neither is forked; both update by
version bump ([updating-from-upstream.md](updating-from-upstream.md)).
