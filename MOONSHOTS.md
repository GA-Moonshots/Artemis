# Artemis

GA-Moonshots' FTC starting kit. Fork it each season, build that year's robot on top, fold what you
learn back in for next year — the part MoonBase never quite managed.

Artemis, goddess of the moon, patron of a Moonshots codebase. She also doesn't miss.

## What you get out of the box

- **Mecanum drive on Pedro Pathing**, field-centric, with the robot and its breadcrumb trail drawn
  live to the Panels dashboard
- **Command-pattern scaffolding** — subsystems, a polymorphic drive-command family, button
  bindings, teleop and autonomous entry points that already run
- **A single telemetry funnel** that goes to the Driver Station and dashboard at once
- **AprilTag localization** that corrects odometry drift when it's confident, and quietly does
  nothing when the camera is missing or unsure
- **Every tunable number in one place**, with the tuning sequence written next to it

## Start here

- Understanding the code → [docs/architecture.md](docs/architecture.md)
- New robot, first drive → [docs/tuning.md](docs/tuning.md)
- Android Studio complaining → [docs/gradle-and-android-studio.md](docs/gradle-and-android-studio.md)
- Working with an AI agent → [AGENTS.md](AGENTS.md)

Standing on the shoulders of [SolversLib](https://docs.seattlesolvers.com) (Seattle Solvers) and
[Pedro Pathing](https://pedropathing.com) — this kit is a thin, opinionated layer over their work,
and we're grateful for it.

## Why not README.md?

`README.md` belongs to upstream — every `git merge upstream/master` rewrites it. Team content lives
in files upstream doesn't know exist, so those merges stay silent. Same reasoning throughout the
repo: [docs/updating-from-upstream.md](docs/updating-from-upstream.md).
