package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathBuilder;

import org.firstinspires.ftc.teamcode.MyRobot;
import org.firstinspires.ftc.teamcode.utils.Constants;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        DRIVE TO POSE                                      ║
 * ║                                                                           ║
 * ║  "Go stand exactly there, facing exactly that way." The workhorse of      ║
 * ║  autonomous — most routines are a stack of these in a                     ║
 * ║  SequentialCommandGroup.                                                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Note how empty execute() is. We hand the follower a path during
 * initialize() and it does the actual driving from PedroDrive.periodic().
 * Commands say WHAT; the follower handles HOW.
 */
public class DriveToPose extends DriveAbstract {

    private final Pose targetPose;
    private boolean arrived = false;

    public DriveToPose(MyRobot robot, Pose target, double timeoutSeconds) {
        super(robot, timeoutSeconds);
        this.targetPose = target;
    }

    @Override
    public void initialize() {
        patience.start();
        arrived = false;

        PathBuilder path = new PathBuilder(follower)
                .addPath(new BezierLine(drive.getPose(), targetPose))
                .setConstantHeadingInterpolation(targetPose.getHeading());
        follower.followPath(path.build());

        robot.sensors.addTelemetry("DriveToPose", "→ (%.1f, %.1f)",
                targetPose.getX(), targetPose.getY());
    }

    @Override
    public void execute() {
        if (follower.atPose(targetPose, Constants.POSE_TOLERANCE, Constants.POSE_TOLERANCE)) {
            arrived = true;
        }

        Pose current = drive.getPose();
        robot.sensors.addTelemetry("Distance Remaining", "%.1f\"",
                Math.hypot(targetPose.getX() - current.getX(),
                           targetPose.getY() - current.getY()));
    }

    @Override
    public boolean isFinished() {
        // Either we got there, or we've run out of patience. Both end the command.
        return arrived || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        standardCleanup();
        robot.sensors.addTelemetry("DriveToPose",
                interrupted ? "INTERRUPTED" : (arrived ? "Arrived" : "TIMED OUT"));
    }
}
