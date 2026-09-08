package org.firstinspires.ftc.teamcode.subsystems;

import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.pedropathing.geometry.Pose;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.MyRobot;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.FieldMap;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                         SENSORS SUBSYSTEM                                 ║
 * ║                                                                           ║
 * ║  Two jobs:                                                                ║
 * ║    • Own every sensor that isn't bolted to a specific mechanism           ║
 * ║      (the Limelight lives here; add colour/distance sensors here too)     ║
 * ║    • Be the ONLY thing in this entire project that flushes telemetry      ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * !!! THIS IS THE ONLY telemetry.update() IN THE WHOLE PROJECT !!!
 *
 * Everyone else calls robot.sensors.addTelemetry(...) and walks away. The
 * scheduler calls periodic() once per loop and that's when it actually ships.
 * Call update() somewhere else too and you get half a frame of data, a
 * mysteriously empty Driver Station, and an afternoon you'll never get back.
 * Ask last year's team how they found this out.
 *
 * Goes to the Driver Station AND the Panels dashboard at the same time.
 */
public class Sensors extends SubsystemBase {

    private final MyRobot robot;
    private final Telemetry telemetry;
    private final TelemetryManager megaphone;

    // ---- Loop timing ----
    // Pedro integrates position using the time between updates, so a slow or
    // stuttering loop degrades localization directly. A healthy FTC loop is
    // well under 20ms. If "Loop (ms)" starts climbing, that is usually the
    // real cause of "the robot drifted" — not the PID tuning everyone blames.
    private long lastLoopNanos = 0;
    private double loopMs = 0;
    private double worstLoopMs = 0;
    private final ElapsedTime matchClock = new ElapsedTime();

    // ---- Vision ----
    // null means "no camera today", and that is a completely acceptable state.
    // Every call below is guarded. A vision failure that stops the robot is
    // worse than no vision at all.
    private Limelight3A limelight = null;
    private String visionVerdict = "starting up";
    private Pose lastAcceptedPose = null;
    private int acceptedCount = 0;
    private int rejectedCount = 0;

    public Sensors(MyRobot robot) {
        this.robot = robot;
        this.telemetry = robot.telemetry;
        this.megaphone = PanelsTelemetry.INSTANCE.getTelemetry();

        initLimelight();

        // Add the rest of this year's sensors here — colour, distance, touch.
        // Wrap anything that might not be plugged in in a try/catch and leave
        // it null; a missing sensor should degrade the robot, not brick it
        // thirty seconds before a match.
    }

    // ============================================================
    //                        TELEMETRY
    // ============================================================

    /** Queue one line. Prints nothing until periodic() flushes. */
    public void addTelemetry(String key, String value) {
        megaphone.addData(key, value);
    }

    /** Same, but with String.format built in: addTelemetry("X", "%.1f\"", x) */
    public void addTelemetry(String key, String format, Object... args) {
        megaphone.addData(key, String.format(format, args));
    }

    /** Seconds since this OpMode's Sensors was built. Teleop is 120s. */
    public double matchSeconds() {
        return matchClock.seconds();
    }

    public double loopMs() {
        return loopMs;
    }

    // ============================================================
    //                        PERIODIC
    // ============================================================

    @Override
    public void periodic() {
        trackLoopTime();
        updateVision();

        addTelemetry("═══ Health ═══", "");
        addTelemetry("Match (s)", "%.0f", matchClock.seconds());
        addTelemetry("Loop (ms)", "%.1f  (worst %.1f)", loopMs, worstLoopMs);

        // If the field drawing gave up, say so. A blank map with no explanation
        // sends people hunting for a localization bug that isn't there.
        String drawFail = robot.drive == null ? null : robot.drive.drawingDisabledReason();
        if (drawFail != null) {
            addTelemetry("Panels drawing", "OFF — " + drawFail);
        }
        reportVision();

        // The one flush. Sends to Panels and the Driver Station together.
        megaphone.update(telemetry);
    }

    private void trackLoopTime() {
        long now = System.nanoTime();
        if (lastLoopNanos != 0) {
            loopMs = (now - lastLoopNanos) / 1_000_000.0;
            // Skip the first few loops — startup is always slow and would
            // otherwise pin "worst" at a number that never happens again.
            if (matchClock.seconds() > 1.0 && loopMs > worstLoopMs) {
                worstLoopMs = loopMs;
            }
        }
        lastLoopNanos = now;
    }

    // ============================================================
    //                     VISION / APRILTAGS
    // ============================================================

    /** Is there a camera at all? Everything else must cope when this is false. */
    public boolean hasCamera() {
        return limelight != null;
    }

    /** Most recent pose we actually believed, or null. */
    public Pose lastAcceptedPose() {
        return lastAcceptedPose;
    }

    private void initLimelight() {
        try {
            limelight = robot.hardwareMap.get(Limelight3A.class, Constants.LIMELIGHT_NAME);
            limelight.setPollRateHz(Constants.LIMELIGHT_POLL_HZ);
            limelight.pipelineSwitch(Constants.LIMELIGHT_APRILTAG_PIPELINE);
            limelight.start();

            // Push our field map so tag coordinates live in git rather than
            // only on the camera. Best effort — a camera with a good built-in
            // map still works, so we don't fail the robot over this.
            try {
                limelight.uploadFieldmap(FieldMap.buildLimelightFieldMap(), null);
            } catch (Exception ignored) {
                // Older firmware, or the map was rejected. Carry on.
            }
        } catch (Exception e) {
            limelight = null;
            visionVerdict = "no camera";
        }
    }

    private void updateVision() {
        if (limelight == null) return;

        try {
            // MegaTag2 needs to know which way we're facing. Pedro already
            // knows, so hand it over every loop — skip this and MT2 quietly
            // returns worse numbers rather than an error.
            limelight.updateRobotOrientation(Math.toDegrees(robot.drive.getNormalizedHeading()));
            evaluate(limelight.getLatestResult());
        } catch (Exception e) {
            // Camera died mid-match. Stop talking to it; keep driving.
            limelight = null;
            visionVerdict = "camera died — odometry only";
        }
    }

    /**
     * The trust policy: snap, but only when confident. Every early return is a
     * reason we declined, and each one shows up on the dashboard — a rejection
     * reason teaches you more than a silent correction ever will.
     *
     * Note what this deliberately does NOT do: demand a particular number of
     * tags. Some fields carry only two localization tags, at opposite ends, so
     * "need three at once" would mean never correcting. Tag count is a signal,
     * not a gate.
     */
    private void evaluate(LLResult result) {
        if (result == null || !result.isValid()) {
            rejectVision("no valid target");
            return;
        }
        if (result.getStaleness() > Constants.VISION_MAX_STALENESS_MS) {
            rejectVision(String.format("stale (%dms)", result.getStaleness()));
            return;
        }

        int tagCount = result.getBotposeTagCount();
        if (tagCount < 1) {
            rejectVision("no tags in view");
            return;
        }

        // Far tags are noisy tags: small angular error becomes large position
        // error with distance.
        double avgDist = FieldMap.metersToInches(result.getBotposeAvgDist());
        if (avgDist > Constants.VISION_MAX_TAG_DISTANCE_INCHES) {
            rejectVision(String.format("too far (%.0f\")", avgDist));
            return;
        }

        Pose3D botpose = result.getBotpose_MT2();
        if (botpose == null) {
            rejectVision("no MT2 pose (is heading being sent?)");
            return;
        }

        Pose visionPose = FieldMap.limelightToPedro(botpose);

        // A pose off the field is a bad read, full stop.
        if (visionPose.getX() < -12 || visionPose.getX() > 156
                || visionPose.getY() < -12 || visionPose.getY() > 156) {
            rejectVision("off-field reading");
            return;
        }

        // The big one: how far is this from where we think we are? A small
        // disagreement is drift worth fixing. A large one is far more likely a
        // misread than the robot having teleported.
        Pose current = robot.drive.getPose();
        double jump = Math.hypot(visionPose.getX() - current.getX(),
                                 visionPose.getY() - current.getY());
        if (jump > Constants.VISION_MAX_JUMP_INCHES) {
            rejectVision(String.format("implausible jump (%.0f\")", jump));
            return;
        }

        if (Tunables.VISION_CORRECTIONS_ENABLED) {
            robot.drive.setPose(visionPose);
        }
        lastAcceptedPose = visionPose;
        acceptedCount++;
        visionVerdict = String.format("SNAP %.1f\" (%d tag%s)",
                jump, tagCount, tagCount == 1 ? "" : "s");
    }

    private void rejectVision(String why) {
        rejectedCount++;
        visionVerdict = "reject: " + why;
    }

    private void reportVision() {
        addTelemetry("═══ Vision ═══", "");
        if (limelight == null) {
            addTelemetry("Camera", "OFFLINE — driving on odometry");
            return;
        }

        addTelemetry("Status", visionVerdict);
        addTelemetry("Corrections", "%d taken / %d declined", acceptedCount, rejectedCount);
        if (!Tunables.VISION_CORRECTIONS_ENABLED) {
            addTelemetry("Vision mode", "WATCHING ONLY (not correcting)");
        }

        // Draw the tags we know about, so a frame problem is visible rather
        // than theoretical.
        if (Tunables.SHOW_TAGS) {
            for (FieldMap.Tag tag : FieldMap.localizationTags()) {
                robot.drive.drawTagSighting(tag.x, tag.y);
            }
        }
    }
}
