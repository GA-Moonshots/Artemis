package org.firstinspires.ftc.teamcode.utils;

import com.bylazar.field.FieldManager;
import com.bylazar.field.PanelsField;
import com.bylazar.field.Style;
import com.pedropathing.geometry.Pose;
import com.pedropathing.math.Vector;
import com.pedropathing.util.PoseHistory;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                            FIELD VIEW                                     ║
 * ║                                                                           ║
 * ║  Everything we draw on the Panels dashboard, in one place.                ║
 * ║                                                                           ║
 * ║  If you can see it, you can debug it. A path that's wrong on the field    ║
 * ║  is almost always obviously wrong on the dashboard first.                 ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  READ THIS BEFORE YOU TRUST A COORDINATE YOU HOVERED
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Panels' canvas and Pedro's field are NOT the same coordinate system.
 *
 *   Pedro:  0..144 inches, origin at a CORNER, 0 rad points +X (right)
 *   Panels: origin at the field CENTER
 *
 * The bridge is a preset. The "Pedro Pathing" preset is:
 *
 *       offsetX = -72   offsetY = -72   rotation = 90°   flipY = true
 *
 * That is not a small adjustment — it's a shift, a rotation, AND a mirror. If
 * the preset selected in the Panels UI doesn't match the one we push from
 * code, hovering the map gives you a number in a different frame than the
 * robot drives in. Everything still looks self-consistent, which is what makes
 * it so expensive: last season ended up with hand-fudged field bounds like
 * "Y from -86 to 144" because the numbers were tuned until they matched
 * instead of being understood.
 *
 * We pin the preset here, in code, so the UI can't quietly disagree — and we
 * draw the frame markers below so you can SEE whether it's right in about two
 * seconds, without moving the robot.
 */
public class FieldView {

    // Robot
    private static final double ROBOT_RADIUS = 9.0;
    private static final Style RED_ROBOT   = new Style("", "#EF5350", 3.0);
    private static final Style BLUE_ROBOT  = new Style("", "#42A5F5", 3.0);
    private static final Style BREADCRUMBS = new Style("", "#81C784", 2.0);

    // Where the robot is trying to get to, vs. where it actually is
    private static final Style TARGET = new Style("", "#FFB300", 2.0);

    // Frame-of-reference markers
    private static final Style AXIS_X = new Style("", "#E53935", 3.0);  // +X, red
    private static final Style AXIS_Y = new Style("", "#43A047", 3.0);  // +Y, green
    private static final Style LANDMARK = new Style("", "#9E9E9E", 2.0);

    private FieldManager field;
    private boolean enabled = true;

    public FieldView() {
        try {
            field = PanelsField.INSTANCE.getField();
            // Pin the frame in code. Do not rely on the dashboard dropdown.
            field.setOffsets(PanelsField.INSTANCE.getPresets().getPEDRO_PATHING());
        } catch (Exception e) {
            // No dashboard? Fine. Drawing is a luxury; driving is not.
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean on) {
        enabled = on;
    }

    // ============================================================
    //             FRAME OF REFERENCE  (the important one)
    // ============================================================

    /**
     * Draws the coordinate frame itself: origin, both axes, field center, and
     * the far corner.
     *
     * HOW TO READ IT — if the frame is right you should see:
     *   • the origin dot sitting in a field CORNER (Pedro's 0,0)
     *   • a RED arm running 24" along +X from it
     *   • a GREEN arm running 24" along +Y from it
     *   • a dot dead centre of the field (72,72)
     *   • a dot in the opposite corner (144,144)
     *
     * If any of those land somewhere else, the Panels preset and Pedro's frame
     * disagree — fix that BEFORE you tune anything or write a path, because
     * every coordinate you hover after this point is a lie.
     */
    public void drawFrameMarkers() {
        if (!enabled || field == null) return;
        try {
            // Origin + axes
            field.setStyle(AXIS_X);
            field.moveCursor(0, 0);
            field.line(24, 0);          // +X arm

            field.setStyle(AXIS_Y);
            field.moveCursor(0, 0);
            field.line(0, 24);          // +Y arm

            field.setStyle(LANDMARK);
            field.moveCursor(0, 0);
            field.circle(3);            // origin

            field.moveCursor(72, 72);
            field.circle(3);            // field centre

            field.moveCursor(144, 144);
            field.circle(3);            // far corner
        } catch (Exception e) {
            enabled = false;
        }
    }

    // ============================================================
    //                        ROBOT
    // ============================================================

    /** Circle for the body, line for the nose. Coloured by alliance. */
    public void drawRobot(Pose pose, boolean isRed) {
        if (!enabled || field == null || !isSane(pose)) return;
        try {
            Style style = isRed ? RED_ROBOT : BLUE_ROBOT;

            field.setStyle(style);
            field.moveCursor(pose.getX(), pose.getY());
            field.circle(ROBOT_RADIUS);

            Vector heading = pose.getHeadingAsUnitVector();
            heading.setMagnitude(heading.getMagnitude() * ROBOT_RADIUS);

            field.setStyle(style);
            field.moveCursor(pose.getX() + heading.getXComponent() / 2,
                             pose.getY() + heading.getYComponent() / 2);
            field.line(pose.getX() + heading.getXComponent(),
                       pose.getY() + heading.getYComponent());
        } catch (Exception e) {
            enabled = false;
        }
    }

    /** Where we've been this match. */
    public void drawBreadcrumbs(PoseHistory history) {
        if (!enabled || field == null || history == null) return;
        try {
            double[] xs = history.getXPositionsArray();
            double[] ys = history.getYPositionsArray();
            if (xs == null || ys == null) return;

            field.setStyle(BREADCRUMBS);
            int size = Math.min(xs.length, ys.length);
            for (int i = 0; i < size - 1; i++) {
                field.moveCursor(xs[i], ys[i]);
                field.line(xs[i + 1], ys[i + 1]);
            }
        } catch (Exception e) {
            enabled = false;
        }
    }

    /**
     * Where the robot is TRYING to go, plus a line from where it is.
     * Seeing intent next to reality turns "why did it do that" into a picture.
     */
    public void drawTarget(Pose target, Pose current) {
        if (!enabled || field == null || !isSane(target)) return;
        try {
            field.setStyle(TARGET);
            field.moveCursor(target.getX(), target.getY());
            field.circle(ROBOT_RADIUS / 2);

            if (isSane(current)) {
                field.setStyle(TARGET);
                field.moveCursor(current.getX(), current.getY());
                field.line(target.getX(), target.getY());
            }
        } catch (Exception e) {
            enabled = false;
        }
    }

    /** An AprilTag we can see, and the line of sight to it. */
    public void drawTagSighting(double tagX, double tagY, Pose robot) {
        if (!enabled || field == null) return;
        try {
            field.setStyle(TARGET);
            field.moveCursor(tagX, tagY);
            field.rect(4, 4);

            if (isSane(robot)) {
                field.moveCursor(robot.getX(), robot.getY());
                field.line(tagX, tagY);
            }
        } catch (Exception e) {
            enabled = false;
        }
    }

    /** Push everything queued above to the dashboard. Once per loop. */
    public void send() {
        if (!enabled || field == null) return;
        try {
            field.update();
        } catch (Exception e) {
            enabled = false;
        }
    }

    /** NaN poses show up when localization is confused; drawing one kills the canvas. */
    private boolean isSane(Pose p) {
        return p != null
                && !Double.isNaN(p.getX())
                && !Double.isNaN(p.getY())
                && !Double.isNaN(p.getHeading());
    }
}
