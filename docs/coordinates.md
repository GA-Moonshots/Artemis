# Coordinates: the frame of reference

Read this before you write a path, tune anything, or trust a number you hovered over on the
dashboard. Getting this wrong is silent — everything stays self-consistent while being wrong, which
is why it costs whole seasons rather than afternoons.

## The one frame we use

**Pedro's.** Inches, origin at a field corner, and headings in radians.

```
        y=144 ┌─────────────────────┐
              │                     │
              │      (72,72)        │     0 rad  → +X (right)
              │       centre        │    π/2 rad → +Y (up)
              │                     │    ±π rad  → -X (left)
        y=0   └─────────────────────┘   -π/2 rad → -Y (down)
             x=0                  x=144
```

Everything in `Constants` and every `Pose` in this repo is in that frame. When another system hands
us coordinates, we convert at the boundary — never halfway through a command.

## Why Panels can lie to you

Panels' canvas is **field-centre origin**. Ours is **corner origin**. The bridge is a preset, and
the Pedro one is not a small nudge:

```
PEDRO_PATHING = offsetX -72, offsetY -72, rotation 90°, flipY true
PANELS / DEFAULT_FTC / ROAD_RUNNER = offset 0,0, no rotation
```

A shift, a rotation, *and* a mirror. Pick a different preset in the dashboard UI and the hover
readout is still perfectly consistent with itself — just in a different frame than the robot drives
in. That's the trap: nothing errors, the numbers just quietly mean something else.

Last season this produced hand-fudged field bounds like `Y from -86 to 144` on a 144-inch field.
Those numbers weren't wrong so much as *derived from the wrong frame and then patched until they
matched*.

We pin the preset in code — [`PedroDrive`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/PedroDrive.java)
sets it at construction — so the dropdown can't silently disagree.

## Checking it in two seconds

`PedroDrive` draws the frame itself, every loop. Look at the dashboard:

| You should see | Meaning |
|---|---|
| Grey dot in a **corner** | Pedro's (0,0) |
| **Red** arm, 24" from it | +X direction |
| **Green** arm, 24" from it | +Y direction |
| Grey dot **dead centre** | (72,72) |
| Grey dot in the **opposite corner** | (144,144) |

If the origin dot isn't in a corner, or the red arm points the wrong way, **stop.** The frame is
wrong and every coordinate you write from here is fiction. No robot movement needed to find this
out — that's the entire point.

Turn the markers off with `SHOW_FRAME_MARKERS` in [`Tunables`](../TeamCode/src/main/java/org/firstinspires/ftc/teamcode/utils/Tunables.java)
once you trust it. Turn them back on the moment anything looks odd.

## The end-to-end test

Frame markers prove Panels agrees with itself. This proves the *robot* agrees too:

1. Hover a spot on the Panels field. Note the coordinate.
2. Put that coordinate in a `DriveToPose` and run it.
3. The robot should stop where you pointed.

If it drives somewhere else, the mismatch is between Pedro and Panels — not your path code.

## Other frames, and where we convert

| Source | Its frame | Where we convert |
|---|---|---|
| Panels canvas | centre origin, rotated, Y-flipped | `PedroDrive` (preset, pinned in code) |
| Limelight botpose | metres, centre origin | `FieldMap.limelightToPedro()` |
| FTC SDK AprilTag | inches, centre origin (±72) | `FieldMap.ftcToPedro()` |

The rule: **convert once, at the edge.** A conversion buried in the middle of a command is a bug
waiting for a Saturday.
