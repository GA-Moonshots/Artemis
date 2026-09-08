package org.firstinspires.ftc.teamcode.subsystems;

import com.bylazar.field.FieldManager;
import com.bylazar.field.PanelsField;
import com.bylazar.field.Style;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.math.Vector;
import com.pedropathing.util.PoseHistory;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.MyRobot;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        PEDRO DRIVE SUBSYSTEM                              ║
 * ║                                                                           ║
 * ║  Mecanum drivetrain + Pedro Pathing. We run mecanum every year, so        ║
 * ║  this file survives the season rollover. Tune it, don't rewrite it.       ║
 * ║                                                                           ║
 * ║  Features:                                                                ║
 * ║    • Field-centric and robot-centric drive                                ║
 * ║    • Pedro Pathing localization (Pinpoint odometry)                       ║
 * ║    • Slow mode for precision                                              ║
 * ║    • Live robot + breadcrumb trail drawn to the Panels dashboard          ║
 * ║    • Every number it uses lives in Constants.java                         ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class PedroDrive extends SubsystemBase {

    // ============================================================
    //                     CORE COMPONENTS
    // ============================================================

    private final MyRobot robot;

    /** Pedro's follower: does localization AND path following. */
    public final Follower follower;

    private final DcMotorEx leftFront, leftBack, rightFront, rightBack;
    private final IMU imu;

    // ============================================================
    //                    DRIVE CONFIGURATION
    // ============================================================

    private boolean fieldCentric = Constants.DEFAULT_FIELD_CENTRIC;
    private double driveSpeed = Constants.DEFAULT_DRIVE_SPEED;

    // ============================================================
    //                    DASHBOARD DRAWING
    // ============================================================
    //
    //  Panels' canvas and Pedro's field are NOT the same coordinate system.
    //
    //     Pedro:  0..144 inches, origin at a CORNER, 0 rad = +X
    //     Panels: origin at the field CENTRE
    //
    //  The bridge is a preset, and it is not a small adjustment:
    //
    //     offsetX -72, offsetY -72, rotation 90°, flipY true
    //
    //  A shift, a rotation, AND a mirror. If the preset picked in the Panels UI
    //  doesn't match the one we push from code, hovering the map hands you a
    //  number in a different frame than the robot drives in — self-consistent,
    //  and completely wrong. That's why we pin it here instead of trusting the
    //  dropdown, and why drawFrameMarkers() exists. See docs/coordinates.md.

    private static final double ROBOT_RADIUS = 9.0;
    private static final Style RED_ROBOT   = new Style("", "#EF5350", 3.0);
    private static final Style BLUE_ROBOT  = new Style("", "#42A5F5", 3.0);
    private static final Style BREADCRUMBS = new Style("", "#81C784", 2.0);
    private static final Style TARGET      = new Style("", "#FFB300", 2.0);
    private static final Style AXIS_X      = new Style("", "#E53935", 3.0);  // +X, red
    private static final Style AXIS_Y      = new Style("", "#43A047", 3.0);  // +Y, green
    private static final Style LANDMARK    = new Style("", "#9E9E9E", 2.0);

    private FieldManager panels;
    private boolean drawingEnabled = true;

    /**
     * Why drawing switched itself off, if it did. A blank map with no
     * explanation sends people hunting a localization bug that isn't there,
     * so Sensors puts this on telemetry.
     */
    private String drawingDisabledReason = null;

    /** Where the active command is trying to go. Null when nothing is driving. */
    private Pose targetPose = null;

    // ============================================================
    //                        CONSTRUCTOR
    // ============================================================

    /**
     * @param robot     the robot that owns this subsystem
     * @param startPose where we're telling the robot it currently is. Auto sets
     *                  this from the alliance; TeleOp inherits whatever Auto
     *                  left behind (or a default if Auto never ran).
     */
    public PedroDrive(MyRobot robot, Pose startPose) {
        this.robot = robot;

        // ============ Motors ============
        leftFront  = robot.hardwareMap.get(DcMotorEx.class, Constants.LEFT_FRONT_NAME);
        leftBack   = robot.hardwareMap.get(DcMotorEx.class, Constants.LEFT_BACK_NAME);
        rightFront = robot.hardwareMap.get(DcMotorEx.class, Constants.RIGHT_FRONT_NAME);
        rightBack  = robot.hardwareMap.get(DcMotorEx.class, Constants.RIGHT_BACK_NAME);

        leftFront.setDirection(Constants.LEFT_FRONT_DIRECTION);
        leftBack.setDirection(Constants.LEFT_BACK_DIRECTION);
        rightFront.setDirection(Constants.RIGHT_FRONT_DIRECTION);
        rightBack.setDirection(Constants.RIGHT_BACK_DIRECTION);

        leftFront.setZeroPowerBehavior(Constants.DRIVE_ZERO_POWER_BEHAVIOR);
        leftBack.setZeroPowerBehavior(Constants.DRIVE_ZERO_POWER_BEHAVIOR);
        rightFront.setZeroPowerBehavior(Constants.DRIVE_ZERO_POWER_BEHAVIOR);
        rightBack.setZeroPowerBehavior(Constants.DRIVE_ZERO_POWER_BEHAVIOR);

        // ============ IMU ============
        imu = robot.hardwareMap.get(IMU.class, Constants.IMU_NAME);
        imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                Constants.IMU_LOGO_DIRECTION,
                Constants.IMU_USB_DIRECTION
        )));

        // ============ Pedro ============
        follower = Constants.createFollower(robot.hardwareMap);
        follower.setStartingPose(startPose);

        // ============ Dashboard ============
        try {
            panels = PanelsField.INSTANCE.getField();
            // Pin the frame in code. Do not rely on the dashboard dropdown.
            panels.setOffsets(PanelsField.INSTANCE.getPresets().getPEDRO_PATHING());
        } catch (Exception e) {
            // No dashboard? Fine. Drawing is a luxury; driving is not.
            disableDrawing("no dashboard at startup: " + e.getClass().getSimpleName());
        }
    }

    // ============================================================
    //                    PERIODIC
    // ============================================================

    @Override
    public void periodic() {
        // Pedro's localization update. EXACTLY ONCE PER LOOP.
        // Not zero times (robot thinks it never moved).
        // Not twice (robot thinks it moved twice as far). Once.
        follower.update();

        draw();
        addTelemetry();
    }

    // ============================================================
    //                    DRIVE CONTROL
    // ============================================================

    /**
     * Mecanum teleop drive. All three inputs are -1.0 to 1.0.
     *
     * @param forward +1 is away from the driver
     * @param strafe  +1 is left
     * @param turn    +1 is counter-clockwise
     */
    public void drive(double forward, double strafe, double turn) {
        // Pedro's flag is robotCentric, which is the opposite of ours. Hence the !.
        follower.setTeleOpDrive(
                forward * driveSpeed,
                strafe * driveSpeed,
                turn * driveSpeed,
                !fieldCentric
        );
    }

    /** Everything stops. Right now. */
    public void stop() {
        leftFront.setPower(0);
        leftBack.setPower(0);
        rightFront.setPower(0);
        rightBack.setPower(0);
    }

    // ============================================================
    //                    LOCALIZATION
    // ============================================================

    public Pose getPose() {
        return follower.getPose();
    }

    /** Teleport the robot's *belief* about where it is (e.g. after an AprilTag fix). */
    public void setPose(Pose newPose) {
        follower.setPose(newPose);
    }

    /**
     * Heading wrapped to [-π, π].
     *
     * CRITICAL: Pedro thinks in RADIANS. Math.PI is 180°. If you hand it
     * degrees it will not complain — it will just drive somewhere surprising.
     */
    public double getNormalizedHeading() {
        double angle = follower.getPose().getHeading();
        while (angle > Math.PI)   angle -= 2 * Math.PI;
        while (angle <= -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    /** "Forward is that way now." Resets heading, keeps position. */
    public void resetHeading() {
        imu.resetYaw();
        Pose current = getPose();
        setPose(new Pose(current.getX(), current.getY(), 0));
    }

    /** Straight-line inches to our alliance's scoring target. */
    public double getDistanceToGoal() {
        Pose current = getPose();
        double targetX = robot.isRed ? Constants.RED_TARGET_X : Constants.BLUE_TARGET_X;
        double targetY = robot.isRed ? Constants.RED_TARGET_Y : Constants.BLUE_TARGET_Y;
        return Math.hypot(targetX - current.getX(), targetY - current.getY());
    }

    // ============================================================
    //                    MODES
    // ============================================================

    public void toggleFieldCentric() {
        fieldCentric = !fieldCentric;
    }

    public boolean isFieldCentric() {
        return fieldCentric;
    }

    public void setDriveSpeed(double speed) {
        driveSpeed = Math.max(Constants.MIN_DRIVE_SPEED,
                     Math.min(Constants.MAX_DRIVE_SPEED, speed));
    }

    public double getDriveSpeed() {
        return driveSpeed;
    }

    // ============================================================
    //                    DRAWING
    // ============================================================

    /**
     * One draw pass per loop. Each piece is switchable from the dashboard via
     * Tunables, so a busy canvas can be thinned without a redeploy.
     */
    private void draw() {
        if (!drawingEnabled || panels == null) return;

        try {
            if (Tunables.SHOW_FRAME_MARKERS) drawFrameMarkers();
            if (Tunables.SHOW_BREADCRUMBS)   drawBreadcrumbs(follower.getPoseHistory());
            if (Tunables.SHOW_TARGET && targetPose != null) drawTarget(targetPose);
            drawRobot(getPose());
            panels.update();
        } catch (Exception e) {
            disableDrawing("draw failed: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Draws the coordinate frame itself. This is the two-second test for
     * "does Panels agree with Pedro about where (0,0) is?"
     *
     * If the frame is right you should see:
     *   • a grey dot in a field CORNER          (Pedro's 0,0)
     *   • a RED arm running 24" along +X
     *   • a GREEN arm running 24" along +Y
     *   • a grey dot dead centre                (72,72)
     *   • a grey dot in the opposite corner     (144,144)
     *
     * Anything else means the frame is wrong, and every coordinate you write
     * from here is fiction. These draw at fixed coordinates, so they appear
     * even when localization is dead — which makes them the fastest way to
     * tell a drawing problem from a Pinpoint problem.
     */
    private void drawFrameMarkers() {
        panels.setStyle(AXIS_X);
        panels.moveCursor(0, 0);
        panels.line(24, 0);

        panels.setStyle(AXIS_Y);
        panels.moveCursor(0, 0);
        panels.line(0, 24);

        panels.setStyle(LANDMARK);
        panels.moveCursor(0, 0);
        panels.circle(3);
        panels.moveCursor(72, 72);
        panels.circle(3);
        panels.moveCursor(144, 144);
        panels.circle(3);
    }

    /** Circle for the body, line for the nose. Coloured by alliance. */
    private void drawRobot(Pose pose) {
        if (!isSane(pose)) return;

        Style style = robot.isRed ? RED_ROBOT : BLUE_ROBOT;
        panels.setStyle(style);
        panels.moveCursor(pose.getX(), pose.getY());
        panels.circle(ROBOT_RADIUS);

        Vector heading = pose.getHeadingAsUnitVector();
        heading.setMagnitude(heading.getMagnitude() * ROBOT_RADIUS);

        panels.setStyle(style);
        panels.moveCursor(pose.getX() + heading.getXComponent() / 2,
                          pose.getY() + heading.getYComponent() / 2);
        panels.line(pose.getX() + heading.getXComponent(),
                    pose.getY() + heading.getYComponent());
    }

    /** Where we've been this match. */
    private void drawBreadcrumbs(PoseHistory history) {
        if (history == null) return;
        double[] xs = history.getXPositionsArray();
        double[] ys = history.getYPositionsArray();
        if (xs == null || ys == null) return;

        panels.setStyle(BREADCRUMBS);
        int size = Math.min(xs.length, ys.length);
        for (int i = 0; i < size - 1; i++) {
            panels.moveCursor(xs[i], ys[i]);
            panels.line(xs[i + 1], ys[i + 1]);
        }
    }

    /** Where a command is trying to go, plus a line from where we are. */
    private void drawTarget(Pose target) {
        if (!isSane(target)) return;
        panels.setStyle(TARGET);
        panels.moveCursor(target.getX(), target.getY());
        panels.circle(ROBOT_RADIUS / 2);

        Pose current = getPose();
        if (isSane(current)) {
            panels.setStyle(TARGET);
            panels.moveCursor(current.getX(), current.getY());
            panels.line(target.getX(), target.getY());
        }
    }

    /** An AprilTag we know about, and the line of sight to it. Called by Sensors. */
    public void drawTagSighting(double tagX, double tagY) {
        if (!drawingEnabled || panels == null) return;
        try {
            panels.setStyle(TARGET);
            panels.moveCursor(tagX, tagY);
            panels.rect(4, 4);

            Pose current = getPose();
            if (isSane(current)) {
                panels.moveCursor(current.getX(), current.getY());
                panels.line(tagX, tagY);
            }
        } catch (Exception e) {
            disableDrawing("tag draw failed: " + e.getClass().getSimpleName());
        }
    }

    /** Commands call this so the dashboard can show intent next to reality. */
    public void setTargetPose(Pose target) {
        this.targetPose = target;
    }

    public void clearTargetPose() {
        this.targetPose = null;
    }

    /** null while drawing is healthy. Sensors reports this. */
    public String drawingDisabledReason() {
        return drawingDisabledReason;
    }

    private void disableDrawing(String why) {
        drawingEnabled = false;
        if (drawingDisabledReason == null) drawingDisabledReason = why;
    }

    /** NaN poses appear when localization is confused; drawing one kills the canvas. */
    private boolean isSane(Pose p) {
        return p != null
                && !Double.isNaN(p.getX())
                && !Double.isNaN(p.getY())
                && !Double.isNaN(p.getHeading());
    }

    // ============================================================
    //                    TELEMETRY
    // ============================================================

    private void addTelemetry() {
        Pose pose = getPose();
        robot.sensors.addTelemetry("═══ Drive ═══", "");
        robot.sensors.addTelemetry("Mode", fieldCentric ? "Field-Centric" : "Robot-Centric");
        robot.sensors.addTelemetry("Speed", "%.0f%%", driveSpeed * 100);
        robot.sensors.addTelemetry("Position", "X:%.1f\" Y:%.1f\"", pose.getX(), pose.getY());
        robot.sensors.addTelemetry("Heading", "%.1f°", Math.toDegrees(pose.getHeading()));
    }
}
