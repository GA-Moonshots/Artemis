# Android Studio setup, and running Claude Code there too

Everyone sets up their own machine — you should be able to write code without waiting for a laptop
to be free. That does mean more than one person is pushing, so: **pull before you start working**,
and push when you stop. A conflict you hit today is five minutes; one you find at a competition is
not.

## Android Studio

1. Install the version FIRST currently recommends for this SDK (check `README.md`'s release
   notes — see [gradle-and-android-studio.md](gradle-and-android-studio.md) for why that's still
   the one thing we read straight from the upstream file).
2. Open the project by pointing Android Studio at the repo root, not `TeamCode/` or
   `FtcRobotController/` individually.
3. Let Gradle sync fully before touching anything else. First sync needs internet — dependencies
   download from `google()`/`mavenCentral()`.
4. If Android Studio suggests upgrading Gradle or AGP during that first sync: decline. See
   [gradle-and-android-studio.md](gradle-and-android-studio.md).
5. Clicked it by accident? No harm done — run `./scripts/check-structure.sh`, and it'll tell you
   exactly which file to put back.

## Claude Code inside Android Studio

Claude Code has an official JetBrains plugin, and Android Studio is a JetBrains IDE, so it works
there too — with one Android-Studio-specific catch.

1. Install the Claude Code CLI separately if it isn't already (`npm install -g @anthropic-ai/claude-code`,
   or see [code.claude.com/docs](https://code.claude.com/docs) for current install instructions) —
   the plugin doesn't bundle it the way the VS Code extension does.
2. In Android Studio: **Settings → Plugins → Marketplace**, search "Claude Code", install, restart.
3. **The catch:** Android Studio ships a JetBrains Runtime without JCEF (the embedded Chromium the
   plugin's chat panel needs). On a stock install, the plugin opens to a guidance panel instead of
   the actual chat. Fix: **Help → Find Action → "Choose Boot Runtime"**, pick a runtime whose name
   contains "JCEF", restart when prompted.
4. Once that's done, the plugin auto-detects the CLI on your `PATH` and binds a shortcut
   (`Cmd+Esc` on macOS) that opens Claude Code with the current file, selection, and diagnostics
   as context.

`AGENTS.md` and `CLAUDE.md` are already in the repo root — Claude Code (and most other coding
agents) picks them up automatically the moment you open the project. Nothing else to configure
there.
