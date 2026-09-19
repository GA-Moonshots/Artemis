# Artemis

An FTC robot-code base from **[Moonshots #21681](https://ga-moonshots.netlify.app/)**: the command
pattern, Pedro Pathing, and a camera that knows where things are, on top of FIRST's own SDK. Fork it
each season, build that year's robot on top, fold what you learn back in for next year.

Artemis, goddess of the moon, patron of a Moonshots codebase. She also doesn't miss.

## What you get out of the box

- **Mecanum drive on Pedro Pathing 3**, field-centric, with the robot, its breadcrumb trail, and
  where it's headed drawn live on the Panels dashboard
- **Command-pattern scaffolding** (SolversLib): subsystems, a drive-command family that always
  times out and cleans up, button bindings, and teleop and autonomous entry points that already run
- **AprilTag tracking** that works whether a game's tags are on the walls or on game pieces:
  range, bearing, and aim-from-a-pivot for any mechanism. Plus localization from fixed tags, off
  until you've proven it
- **One telemetry funnel** to the Driver Station and dashboard at once
- **One coordinate frame**, converted once, at the edge, with on-screen markers that show in two
  seconds whether the dashboard agrees
- **Every tunable number in one place**, with the tuning sequence written next to it
- **Scripts that check, never change**: `doctor.sh`, `build.sh`, `check-structure.sh`

It survives dependency churn by design: SolversLib supplies only the scheduler, Pedro's follower is
reached through three files (`PedroDrive`, `DriveAbstract`, `Constants`), and the SDK arrives by a
merge that stays conflict-free because we never edit FIRST's files.

## Start here

- Understanding the code → [docs/architecture.md](docs/architecture.md)
- New laptop → [docs/setup.md](docs/setup.md)
- New robot, first drive → [docs/tuning.md](docs/tuning.md)
- Camera, tags, aiming → [docs/vision.md](docs/vision.md)
- Working with an AI agent → [AGENTS.md](AGENTS.md)

## Using it on your team

You're welcome to. Fork it, then:

1. **Rename `MyRobot`** to your robot's name. It's the one file where a season-specific name belongs.
2. **Hardware names and directions** in `utils/Constants.java` must match your Driver Station
   configuration.
3. **Tune in order**: [docs/tuning.md](docs/tuning.md). Don't skip ahead: every step is tuned on
   top of the one before.
4. **No Limelight?** Everything still runs. Vision reports "no camera" and the robot drives on
   odometry.
5. Keep `upstream` pointed at `FIRST-Tech-Challenge/FtcRobotController` and merge it each season
   ([docs/updating-from-upstream.md](docs/updating-from-upstream.md)).

A few docs mention our other repos (Ganymede, Iapetus) as history. Ignore them freely.

## Credits

Built by **Moonshots, FTC #21681** — [ga-moonshots.netlify.app](https://ga-moonshots.netlify.app/).

Standing on the shoulders of [SolversLib](https://docs.seattlesolvers.com) (Seattle Solvers),
[Pedro Pathing](https://pedropathing.com), Panels (Lazar), and FIRST's
[FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController). This kit is a
thin, opinionated layer over their work, and we're grateful for it.

## Why not README.md?

`README.md` belongs to FIRST: every `git merge upstream/master` rewrites it. Team content lives in
files upstream doesn't know exist, so those merges stay silent. Same reasoning throughout the
repo: [docs/updating-from-upstream.md](docs/updating-from-upstream.md).
