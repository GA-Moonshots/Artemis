package org.firstinspires.ftc.teamcode;

import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.Robot;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.commands.Drive;
import org.firstinspires.ftc.teamcode.commands.DriveFwdByDist;
import org.firstinspires.ftc.teamcode.commands.ExampleCommand;
import org.firstinspires.ftc.teamcode.subsystems.ExampleSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.PedroDrive;
import org.firstinspires.ftc.teamcode.subsystems.Sensors;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                             MY ROBOT                                      ║
 * ║                                                                           ║
 * ║  The one object that owns everything. Subsystems live here, button        ║
 * ║  bindings live here, the autonomous plan lives here. OpModes do           ║
 * ║  nothing but build one of these and get out of the way.                   ║
 * ║                                                                           ║
 * ║  Two constructors:                                                        ║
 * ║    • TeleOp  — doesn't care about alliance, just drives                   ║
 * ║    • Auto    — needs to know alliance + start position                    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Rename this to whatever this year's robot is called. It's the one file
 * where a season-specific name is actually appropriate.
 */
public class MyRobot extends Robot {

    // Core references
    public LinearOpMode opMode;
    public Telemetry telemetry;
    public HardwareMap hardwareMap;
    public GamepadEx player1;
    public GamepadEx player2;

    // Match configuration
    public boolean isRed;
    public boolean isNearGoal;

    // Subsystems
    public PedroDrive drive;
    public Sensors sensors;
    public ExampleSubsystem grabber;  // template — swap for this year's mechanisms

    public Pose startPose;

    // ============================================================
    //                    TELEOP CONSTRUCTOR
    // ============================================================

    public MyRobot(LinearOpMode opMode) {
        this(opMode, true, true, new Pose(0, 0, 0));
    }

    // ============================================================
    //                  AUTONOMOUS CONSTRUCTOR
    // ============================================================

    public MyRobot(LinearOpMode opMode, boolean isRed, boolean isNearGoal, Pose startPose) {
        this.opMode = opMode;
        this.telemetry = opMode.telemetry;
        this.hardwareMap = opMode.hardwareMap;
        this.isRed = isRed;
        this.isNearGoal = isNearGoal;
        this.startPose = startPose;

        this.player1 = new GamepadEx(opMode.gamepad1);
        this.player2 = new GamepadEx(opMode.gamepad2);

        // Sensors first — everything else wants to log to it.
        sensors = new Sensors(this);
        drive = new PedroDrive(this, startPose);
        grabber = new ExampleSubsystem(this);

        register(drive, sensors, grabber);
    }

    // ============================================================
    //                    TELEOP SETUP
    // ============================================================

    public void initTeleOp() {
        // The default command: runs whenever nothing else claims the wheels.
        drive.setDefaultCommand(new Drive(this));

        /*
         ██████╗ ██╗      █████╗ ██╗   ██╗███████╗██████╗      ██╗
         ██╔══██╗██║     ██╔══██╗╚██╗ ██╔╝██╔════╝██╔══██╗    ███║
         ██████╔╝██║     ███████║ ╚████╔╝ █████╗  ██████╔╝    ╚██║
         ██╔═══╝ ██║     ██╔══██║  ╚██╔╝  ██╔══╝  ██╔══██╗     ██║
         ██║     ███████╗██║  ██║   ██║   ███████╗██║  ██║     ██║
         ╚═╝     ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝     ╚═╝

            DRIVER — moves the robot. Doesn't touch game pieces.
        */

        // A — "forward is THIS way now." Fixes a drifted field-centric heading.
        new GamepadButton(player1, GamepadKeys.Button.A)
                .whenPressed(new InstantCommand(() -> drive.resetHeading()));

        // B — toggle field-centric / robot-centric
        new GamepadButton(player1, GamepadKeys.Button.B)
                .whenPressed(new InstantCommand(() -> drive.toggleFieldCentric()));

        // X — nudge forward 12". Example of scheduling a real Command from a button.
        new GamepadButton(player1, GamepadKeys.Button.X)
                .whenPressed(new DriveFwdByDist(this, 12, 3));

        // DPAD UP — panic button. Drops any path and hands the wheels back.
        new GamepadButton(player1, GamepadKeys.Button.DPAD_UP)
                .whenPressed(new InstantCommand(() -> {
                    drive.follower.breakFollowing();
                    drive.follower.startTeleopDrive();
                }));

        // Right bumper is slow mode — read directly in Drive.execute(), not bound here.

        /*
         ██████╗ ██╗      █████╗ ██╗   ██╗███████╗██████╗     ██████╗
         ██╔══██╗██║     ██╔══██╗╚██╗ ██╔╝██╔════╝██╔══██╗    ╚════██╗
         ██████╔╝██║     ███████║ ╚████╔╝ █████╗  ██████╔╝     █████╔╝
         ██╔═══╝ ██║     ██╔══██║  ╚██╔╝  ██╔══╝  ██╔══██╗    ██╔═══╝
         ██║     ███████╗██║  ██║   ██║   ███████╗██║  ██║    ███████╗
         ╚═╝     ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝    ╚══════╝

            OPERATOR — runs the mechanisms. Never drives.
        */

        // A — instant action, no Command needed. Servo flips are immediate.
        new GamepadButton(player2, GamepadKeys.Button.A)
                .whenPressed(new InstantCommand(() -> grabber.toggle()));

        // Y — a real Command, because this one takes time to finish.
        new GamepadButton(player2, GamepadKeys.Button.Y)
                .whenPressed(new ExampleCommand(this));

        // Bind this year's intake / launcher / arm the same way, then delete
        // the two grabber bindings above.
    }

    // ============================================================
    //                  AUTONOMOUS SETUP
    // ============================================================

    /**
     * This year's autonomous routine. Replace the placeholder with real paths.
     *
     * Sequential = one after another. Parallel = all at once. Nest them freely;
     * that's how a whole autonomous becomes one schedulable object.
     */
    public void initAuto() {
        drive.follower.update();  // know where we are before we move

        new SequentialCommandGroup(
                new DriveFwdByDist(this, 24, 5)
                // ...then the rest of this year's plan.
        ).schedule();
    }
}
