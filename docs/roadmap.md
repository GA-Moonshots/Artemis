# Roadmap

What's planned, what's done. Pick something up — each item is small enough to finish in a session.

Camera decision: **Limelight 3A**. Webcams crash and slow startup; the Limelight has held up over
whole seasons. Non-negotiable consequence: if the camera doesn't load, or dies mid-match, the robot
keeps driving on odometry alone.

## Tier 1 — cheap wins

- [x] `setBulkReading()` in `MyRobot` — one line, real loop-time gain. Pedro's accuracy depends on loop time.
- [x] `DriveTurnBy` / `DriveTurnTo` extending `DriveAbstract` — SolversLib's turn commands have no timeout and no cleanup; ours will.
- [x] Loop-time + match-time telemetry in `Sensors` — a slow loop causes a lot of "it drifted" reports.
- [x] `PersistentPoseManager` — hand the robot's pose from autonomous to teleop. Ganymede had this; Artemis lost it.

## Tier 2 — Panels

- [x] Live-editable `Tunables` (drive feel, tolerances, drawing toggles). PIDs deliberately left to the `Tuning` OpMode — two places to edit one number is how you lose an afternoon.
- [x] Draw target pose + a line from current position — intent vs. reality.
- [x] Alliance-coloured robot; tag-sighting draw ready for Tier 3.
- [x] **Frame-of-reference markers** — origin, both axes, centre, far corner. Catches a Panels/Pedro frame mismatch in two seconds without moving the robot. See docs/coordinates.md.

## Tier 3 — AprilTag localization

- [x] `FieldMap.java` — reads tag positions from the SDK's own library (no hand-typed coordinates, updates itself at kickoff) and pushes them to the camera via `uploadFieldmap()`.
- [x] Coordinate bridge — metres/degrees → Pedro inches/radians, isolated in `FieldMap`. Axis orientation still needs field verification; `CameraCalibration` is how you settle it.
- [x] Camera extrinsics recorded in `Constants` + `CameraCalibration` OpMode that reports the error and how to read it.
- [x] Vision folded into `Sensors` (it's a sensor) — MegaTag2 via `updateRobotOrientation()`, trust policy, hard graceful degradation.

**Trust policy: snap, but only when confident.** Overwrite Pedro's pose only when the detection is
fresh, close, multi-tag, and the implied jump is small. Log every rejection with its reason — you
learn more from why it declined than from when it worked.

## Needs the robot

- [ ] Verify `FieldMap.ftcToPedro()` axis orientation with `CameraCalibration`
- [ ] Confirm `Tunables` live-edit round-trips from the dashboard
- [ ] Confirm `uploadFieldmap()` is accepted by the camera firmware
- [ ] Re-check tag ids/sizes at kickoff — the game changes, the code shouldn't need to

## After the build

- [ ] Full review of docs + agent guidance against what actually got built.
