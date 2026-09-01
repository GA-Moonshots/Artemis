package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathBuilder;

import org.firstinspires.ftc.teamcode.MyRobot;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                     FORWARD BY DISTANCE                                   ║
 * ║                                                                           ║
 * ║  "Go that way N inches." Keeps the current heading. Negative = backward.  ║
 * ║                                                                           ║
 * ║  PEDRO COORDINATE SYSTEM (memorize this, it explains 90% of the           ║
 * ║  autonomous bugs you will ever write):                                    ║
 * ║      0 rad     = facing RIGHT   (+X)                                      ║
 * ║      π/2 rad   = facing UP      (+Y)                                      ║
 * ║      ±π rad    = facing LEFT    (-X)                                      ║
 * ║      -π/2 rad  = facing DOWN    (-Y)                                      ║
 * ║                                                                           ║
 * ║  So for heading θ:   ΔX = distance × cos(θ)                               ║
 * ║                      ΔY = distance × sin(θ)                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Same shape as DriveToPose — the only real difference is that this one
 * computes its target instead of being handed one. That's what the abstract
 * base is for: two commands, one set of safety rails.
 */
public class DriveFwdByDist extends DriveAbstract {

    private final double distance;
    private Pose targetPose;
    private boolean arrived = false;

    /**
     * @param distance inches; positive is forward, negative is backward
     */
    public DriveFwdByDist(MyRobot robot, double distance, double timeoutSeconds) {
        super(robot, timeoutSeconds);
        this.distance = distance;
    }

    @Override
    public void initialize() {
        patience.start();
        arrived = false;

        Pose current = drive.getPose();
        double heading = current.getHeading();

        targetPose = new Pose(
                current.getX() + distance * Math.cos(heading),
                current.getY() + distance * Math.sin(heading),
                heading  // same direction we're already pointed
        );

        PathBuilder path = new PathBuilder(follower)
                .addPath(new BezierLine(current, targetPose))
                .setConstantHeadingInterpolation(heading);
        follower.followPath(path.build());
        drive.setTargetPose(targetPose);   // so the dashboard shows intent vs. reality

        robot.sensors.addTelemetry("FwdByDist", "%.1f\" @ %.1f°",
                distance, Math.toDegrees(heading));
    }

    @Override
    public void execute() {
        if (follower.atPose(targetPose, Tunables.POSE_TOLERANCE, Tunables.POSE_TOLERANCE)) {
            arrived = true;
        }
    }

    @Override
    public boolean isFinished() {
        return arrived || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        standardCleanup();
        drive.clearTargetPose();
        robot.sensors.addTelemetry("FwdByDist",
                interrupted ? "INTERRUPTED" : (arrived ? "Arrived" : "TIMED OUT"));
    }
}
