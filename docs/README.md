# Docs

**New here?** Read [architecture.md](architecture.md). It's one page and it explains the whole repo.

**New robot?** [tuning.md](tuning.md), start to finish, before your first real drive.

**Android Studio yelling at you?** [gradle-and-android-studio.md](gradle-and-android-studio.md).
Short version: decline the upgrade it's suggesting.

---

| | |
|---|---|
| [architecture.md](architecture.md) | Subsystems, commands, the scheduler — how the code fits together |
| [tuning.md](tuning.md) | The 8-phase drivetrain tuning sequence |
| [gradle-and-android-studio.md](gradle-and-android-studio.md) | Build errors and what to ignore |
| [android-studio-and-claude.md](android-studio-and-claude.md) | IDE setup, plus Claude Code inside Android Studio |
| [updating-from-upstream.md](updating-from-upstream.md) | Pulling the new season's SDK without losing our layer |
| [issue-log.md](issue-log.md) | Dated gotchas, so nobody rediscovers them next August |

**Did I break the structure?** `./scripts/check-structure.sh` — reports whether anything upstream
owns got edited. Changes nothing, blocks nothing. Run it before you commit.

Deeper theory — PID intuition, Pedro internals — lives in the team
[GitBook](https://gilmour.online/compsci/competitive-robotics/software-team). These docs stay short
enough to read with one hand on the robot.
