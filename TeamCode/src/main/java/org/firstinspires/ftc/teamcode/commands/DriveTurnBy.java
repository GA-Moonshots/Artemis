package org.firstinspires.ftc.teamcode.commands;

import org.firstinspires.ftc.teamcode.MyRobot;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                          TURN BY DEGREES                                  ║
 * ║                                                                           ║
 * ║  "Rotate 90° left." Relative to wherever the robot is currently facing.   ║
 * ║  For "face THAT direction" regardless of current heading, use DriveTurnTo.║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * SolversLib ships a TurnCommand that does the same rotation. We wrap it
 * instead of using it directly for one reason: theirs is
 *
 *     isFinished() { return !follower.isBusy(); }
 *
 * with no timeout and no end(). Pin the robot against a wall and the follower
 * stays busy forever — that command never finishes, and everything queued
 * behind it never runs. Ours inherits DriveAbstract's patience timer and
 * cleanup, so a stuck turn gives up and hands the wheels back.
 *
 * Degrees in, radians handled internally. Pedro thinks in radians; humans
 * don't.
 */
public class DriveTurnBy extends DriveAbstract {

    private final double degrees;
    private final boolean turnLeft;

    /**
     * @param degrees how far to rotate, always positive — direction is the next argument
     * @param turnLeft true for counter-clockwise
     */
    public DriveTurnBy(MyRobot robot, double degrees, boolean turnLeft, double timeoutSeconds) {
        super(robot, timeoutSeconds);
        this.degrees = Math.abs(degrees);
        this.turnLeft = turnLeft;
    }

    /** Signed convenience: positive turns left, negative turns right. */
    public DriveTurnBy(MyRobot robot, double signedDegrees, double timeoutSeconds) {
        this(robot, Math.abs(signedDegrees), signedDegrees >= 0, timeoutSeconds);
    }

    @Override
    public void initialize() {
        patience.start();
        follower.turn(Math.toRadians(degrees), turnLeft);
        robot.sensors.addTelemetry("TurnBy", "%.0f° %s", degrees, turnLeft ? "left" : "right");
    }

    @Override
    public boolean isFinished() {
        return !follower.isBusy() || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        standardCleanup();
        robot.sensors.addTelemetry("TurnBy",
                interrupted ? "INTERRUPTED" : (patience.done() ? "TIMED OUT" : "Done"));
    }
}
