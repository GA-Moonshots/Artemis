package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.util.Timing;

import org.firstinspires.ftc.teamcode.MyRobot;
import org.firstinspires.ftc.teamcode.subsystems.ExampleSubsystem;

import java.util.concurrent.TimeUnit;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║               EXAMPLE COMMAND  ·  TEMPLATE — DELETE ME                    ║
 * ║                                                                           ║
 * ║  The four lifecycle methods, doing the smallest useful thing:             ║
 * ║    initialize()  once, when scheduled       — start timers, kick off      ║
 * ║    execute()     every loop while active    — nudge, check, report        ║
 * ║    isFinished()  every loop, after execute  — true when done              ║
 * ║    end()         once, on finish OR cancel  — stop motors, clean up       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * NEVER BLOCK IN execute(). No while-loops, no Thread.sleep(). The scheduler
 * calls every active command's execute() once per loop — block in one and you
 * freeze the entire robot, not just this command. Want to wait? Check a timer
 * and return false from isFinished() until it's done, like this file does.
 */
public class ExampleCommand extends CommandBase {

    private final MyRobot robot;
    private final ExampleSubsystem grabber;

    /** Out of patience = command over. See DriveAbstract for the same idea. */
    private final Timing.Timer patience;

    public ExampleCommand(MyRobot robot) {
        this.robot = robot;
        this.grabber = robot.grabber;
        this.patience = new Timing.Timer(2, TimeUnit.SECONDS);

        // Claim the subsystem so nothing else grabs it mid-move.
        addRequirements(grabber);
    }

    @Override
    public void initialize() {
        grabber.open();
        patience.start();
    }

    @Override
    public void execute() {
        robot.sensors.addTelemetry("Grabber closing in", "%d ms", patience.remainingTime());
    }

    @Override
    public boolean isFinished() {
        return patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        // Runs whether we finished or got interrupted. Leave things safe.
        grabber.shut();
    }
}
