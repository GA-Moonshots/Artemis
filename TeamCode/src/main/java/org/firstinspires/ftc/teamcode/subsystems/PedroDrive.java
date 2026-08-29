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
    //                    PANELS FIELD DRAWING
    //  Watching a green dot retrace your autonomous beats squinting at
    //  three numbers on a phone. If you can see it, you can debug it.
    // ============================================================

    private static final double ROBOT_RADIUS = 9.0;
    private static final Style ROBOT_STYLE   = new Style("", "#4CAF50", 3.0); // where it is
    private static final Style HISTORY_STYLE = new Style("", "#81C784", 2.0); // where it's been

    private FieldManager panelsField;
    private boolean breadcrumbsEnabled = true;

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

        initializePanelsField();
    }

    private void initializePanelsField() {
        try {
            panelsField = PanelsField.INSTANCE.getField();
            panelsField.setOffsets(PanelsField.INSTANCE.getPresets().getPEDRO_PATHING());
        } catch (Exception e) {
            // No Panels? Fine. Drawing is a luxury; driving is not.
            breadcrumbsEnabled = false;
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

        drawRobotToPanels();
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
    //                    PANELS DRAWING
    // ============================================================

    private void drawRobotToPanels() {
        if (!breadcrumbsEnabled || panelsField == null) return;

        try {
            Pose pose = follower.getPose();

            // NaN poses happen when localization is confused. Drawing one
            // takes the whole dashboard down with it.
            if (pose == null || Double.isNaN(pose.getX())
                    || Double.isNaN(pose.getY()) || Double.isNaN(pose.getHeading())) {
                return;
            }

            drawBreadcrumbs(follower.getPoseHistory());
            drawRobot(pose);
            panelsField.update();
        } catch (Exception e) {
            // Never let a drawing bug stop the robot. Draw once, fail forever.
            breadcrumbsEnabled = false;
        }
    }

    /** Circle for the body, line for the nose so you can tell which way it faces. */
    private void drawRobot(Pose pose) {
        panelsField.setStyle(ROBOT_STYLE);
        panelsField.moveCursor(pose.getX(), pose.getY());
        panelsField.circle(ROBOT_RADIUS);

        Vector heading = pose.getHeadingAsUnitVector();
        heading.setMagnitude(heading.getMagnitude() * ROBOT_RADIUS);

        panelsField.setStyle(ROBOT_STYLE);
        panelsField.moveCursor(pose.getX() + heading.getXComponent() / 2,
                               pose.getY() + heading.getYComponent() / 2);
        panelsField.line(pose.getX() + heading.getXComponent(),
                         pose.getY() + heading.getYComponent());
    }

    /** Breadcrumb trail of everywhere we've been this match. */
    private void drawBreadcrumbs(PoseHistory history) {
        if (history == null) return;

        double[] xs = history.getXPositionsArray();
        double[] ys = history.getYPositionsArray();
        if (xs == null || ys == null) return;

        panelsField.setStyle(HISTORY_STYLE);
        int size = Math.min(xs.length, ys.length);
        for (int i = 0; i < size - 1; i++) {
            panelsField.moveCursor(xs[i], ys[i]);
            panelsField.line(xs[i + 1], ys[i + 1]);
        }
    }

    public void togglePanelsDrawing() {
        breadcrumbsEnabled = !breadcrumbsEnabled;
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
