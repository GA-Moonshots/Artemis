package org.firstinspires.ftc.teamcode.utils;

import com.bylazar.configurables.PanelsConfigurables;
import com.bylazar.configurables.annotations.Configurable;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                            TUNABLES                                       ║
 * ║                                                                           ║
 * ║  Numbers you can change from the Panels dashboard WHILE THE ROBOT IS      ║
 * ║  RUNNING. Edit, watch, edit again — no rebuild, no redeploy.              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Why this is separate from Constants:
 *
 *   Constants  = things that must not change mid-match. Hardware names, motor
 *                directions, the follower configuration. They're `final`, and
 *                `final` is exactly what makes them safe.
 *   Tunables   = things read fresh every loop, where twiddling a number and
 *                watching the robot respond is the fastest way to find the
 *                right value.
 *
 * Panels can only edit non-final statics, so nothing here is final. That's the
 * trade: convenience in exchange for the compiler no longer protecting you.
 * Don't move a hardware name in here to save a redeploy.
 *
 * These reset to the values below on every restart — the dashboard doesn't
 * write your code. Found a keeper? Type it in here and commit it.
 *
 * Drivetrain PIDs are NOT here: the `Tuning` OpMode already tunes those live,
 * and having two places to edit the same number is how you lose an afternoon.
 */
@Configurable
public class Tunables {

    // ---- Driver feel ----

    /** Held right bumper. Lower = finer control. */
    public static double SLOW_MODE_MULTIPLIER = 0.3;

    /** Sticks never truly centre. Ignore noise below this. */
    public static double INPUT_THRESHOLD = 0.1;

    /** Global speed cap. Drop it for a nervous driver or a top-heavy robot. */
    public static double DRIVE_SPEED = 1.0;

    // ---- Autonomous ----

    /** How close (inches) counts as "arrived" for a path command. */
    public static double POSE_TOLERANCE = 0.5;

    // ---- Dashboard drawing ----

    /** Draw the coordinate frame markers. Leave on until you trust the frame. */
    public static boolean SHOW_FRAME_MARKERS = true;

    /** Draw where commands are trying to go. */
    public static boolean SHOW_TARGET = true;

    /** Trail of where we've been. Turn off if the canvas gets busy. */
    public static boolean SHOW_BREADCRUMBS = true;

    /** Draw known AprilTag positions on the field. */
    public static boolean SHOW_TAGS = true;

    // ---- Vision ----

    /**
     * Master switch for pose correction.
     *
     * Turn this OFF to watch vision without letting it touch the robot: the
     * dashboard still shows what it would have done, and nothing moves because
     * of it. That's the right setting for the first few matches on a new
     * camera mount, and the fastest way to rule vision out when autonomous
     * misbehaves.
     */
    public static boolean VISION_CORRECTIONS_ENABLED = true;

    // ============================================================

    /**
     * Ask Panels to sync this class with whatever the dashboard is showing.
     * Called once when the robot is built.
     *
     * NOT YET VERIFIED ON HARDWARE. The dashboard is expected to write these
     * fields directly as you edit them, with this call acting as the initial
     * sync — that's how the Tuning OpMode uses it. If you edit a value on the
     * dashboard and the robot ignores you, this is the first thing to suspect:
     * try calling refresh() every loop from Sensors.periodic() and see whether
     * that fixes it. If it does, say so in docs/issue-log.md so the next
     * person doesn't have to work it out again.
     */
    public static void refresh() {
        try {
            PanelsConfigurables.INSTANCE.refreshClass(new Tunables());
        } catch (Exception e) {
            // No dashboard connected. The defaults above still apply.
        }
    }
}
