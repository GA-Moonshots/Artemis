package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.MyRobot;
import org.firstinspires.ftc.teamcode.utils.Constants;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║              EXAMPLE SUBSYSTEM  ·  TEMPLATE — DELETE ME                   ║
 * ║                                                                           ║
 * ║  Copy this file when you add a real mechanism (intake, arm, launcher).    ║
 * ║  The shape is the lesson:                                                 ║
 * ║    • hardware lookups in the constructor, never in a loop                 ║
 * ║    • hardware names from Constants, never typed inline                    ║
 * ║    • small public methods that each do exactly one thing                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class ExampleSubsystem extends SubsystemBase {

    private static final double OPEN = 1.0;
    private static final double SHUT = 0.0;

    private final MyRobot robot;
    private final Servo theGrabber;

    private boolean isOpen = false;

    public ExampleSubsystem(MyRobot robot) {
        this.robot = robot;
        theGrabber = robot.hardwareMap.get(Servo.class, Constants.EXAMPLE_SERVO_NAME);
        theGrabber.setPosition(SHUT);
    }

    // Instant actions don't need Commands — bind these straight to a button.
    public void open() {
        theGrabber.setPosition(OPEN);
        isOpen = true;
    }

    public void shut() {
        theGrabber.setPosition(SHUT);
        isOpen = false;
    }

    public void toggle() {
        if (isOpen) shut(); else open();
    }

    @Override
    public void periodic() {
        robot.sensors.addTelemetry("Grabber", isOpen ? "OPEN" : "SHUT");
    }
}
