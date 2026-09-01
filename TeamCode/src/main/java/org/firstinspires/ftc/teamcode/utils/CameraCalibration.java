package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.MyRobot;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                       CAMERA CALIBRATION                                  ║
 * ║                                                                           ║
 * ║  Is the camera telling the truth about where we are?                      ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * HOW TO USE IT (about five minutes):
 *
 *   1. Measure a real spot on the field and put the robot there, precisely.
 *      A field tile corner works well — you know exactly where those are.
 *   2. Set KNOWN_X / KNOWN_Y / KNOWN_HEADING_DEG below (or edit them live from
 *      the Panels dashboard) to that spot in Pedro coordinates.
 *   3. Point the robot so the camera can see a tag. Run this OpMode.
 *   4. Read the ERROR line.
 *
 * WHAT THE ERROR IS TELLING YOU:
 *
 *   Small and steady (< ~1")     → you're calibrated. Go drive.
 *   Constant offset in one axis  → the camera mounting numbers are off by
 *                                  roughly that much. Fix the "robot space"
 *                                  values in the Limelight web UI, and record
 *                                  them in Constants.CAMERA_* so the next
 *                                  reflash doesn't lose them.
 *   X and Y look swapped         → the frames disagree about axes. Fix
 *                                  FieldMap.ftcToPedro(), and nowhere else.
 *   Error grows with distance    → camera angle (pitch) is off, not position.
 *   Wildly unstable              → check lighting and exposure before you
 *                                  change any numbers.
 *
 * This OpMode never moves the robot and never corrects the pose. It only
 * looks and reports, so it's safe to run any time.
 */
@TeleOp(name = "Camera Calibration", group = "Tuning")
public class CameraCalibration extends CommandOpMode {

    /** Where the robot ACTUALLY is, in Pedro coordinates. Measure, don't guess. */
    public static double KNOWN_X = 72.0;
    public static double KNOWN_Y = 72.0;
    public static double KNOWN_HEADING_DEG = 0.0;

    private MyRobot robot;

    @Override
    public void initialize() {
        robot = new MyRobot(this);

        // Look, don't touch: we want to compare against the pose we typed in,
        // not against a pose vision has already "corrected."
        Tunables.VISION_CORRECTIONS_ENABLED = false;

        telemetry.addLine("Park the robot at the known spot, then press START.");
        telemetry.addLine("Make sure the camera can see a tag.");
        telemetry.update();
    }

    @Override
    public void run() {
        super.run();

        Pose truth = new Pose(KNOWN_X, KNOWN_Y, Math.toRadians(KNOWN_HEADING_DEG));
        robot.drive.setPose(truth);   // we know where we are; assert it

        Pose seen = robot.sensors.lastAcceptedPose();

        robot.sensors.addTelemetry("═══ Calibration ═══", "");
        robot.sensors.addTelemetry("Known", "X:%.1f Y:%.1f H:%.0f°",
                KNOWN_X, KNOWN_Y, KNOWN_HEADING_DEG);

        if (!robot.sensors.hasCamera()) {
            robot.sensors.addTelemetry("Camera", "OFFLINE — nothing to calibrate");
            return;
        }
        if (seen == null) {
            robot.sensors.addTelemetry("Camera", "no accepted reading yet — see Vision status");
            return;
        }

        double dx = seen.getX() - KNOWN_X;
        double dy = seen.getY() - KNOWN_Y;
        double dh = Math.toDegrees(seen.getHeading()) - KNOWN_HEADING_DEG;

        robot.sensors.addTelemetry("Camera says", "X:%.1f Y:%.1f H:%.0f°",
                seen.getX(), seen.getY(), Math.toDegrees(seen.getHeading()));
        robot.sensors.addTelemetry("ERROR", "dX:%+.1f\" dY:%+.1f\" dH:%+.0f°", dx, dy, dh);
        robot.sensors.addTelemetry("Distance off", "%.1f\"", Math.hypot(dx, dy));

        // A cheap hint that's easy to miss by eye.
        if (Math.abs(dx - (KNOWN_Y - seen.getY())) < 2.0 && Math.hypot(dx, dy) > 4.0) {
            robot.sensors.addTelemetry("HINT", "looks like X/Y are swapped — see FieldMap.ftcToPedro");
        }
    }
}
