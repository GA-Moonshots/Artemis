package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.control.FilteredPIDFCoefficients;
import com.pedropathing.control.PIDFCoefficients;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                            CONSTANTS                                      ║
 * ║                                                                           ║
 * ║  Every magic string and tunable number the robot owns, in one file.       ║
 * ║  If you're about to type a hardware name in quotes somewhere else,        ║
 * ║  stop and put it here instead. Future you is counting on it.              ║
 * ║                                                                           ║
 * ║  Anything marked ⚙ TUNE is machine-specific and WILL be wrong on a        ║
 * ║  new robot. See docs/tuning.md before your first real drive.              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Note: upstream ships a pedroPathing/Constants.java, but it's a stub that
 * configures no drivetrain and no localizer — a follower built from it can't
 * actually drive. This file is the real one. Ours, so upstream merges never
 * touch it.
 */
public class Constants {

    // ============================================================
    //                    HARDWARE MAP NAMES
    //     Must match the Driver Station config EXACTLY, including case.
    //     A typo here is a NullPointerException three seconds into a match.
    // ============================================================

    public static final String LEFT_FRONT_NAME  = "leftFront";
    public static final String RIGHT_FRONT_NAME = "rightFront";
    public static final String LEFT_BACK_NAME   = "leftBack";
    public static final String RIGHT_BACK_NAME  = "rightBack";

    public static final String IMU_NAME = "imu";

    /** Template — rename when you build this year's actual mechanism. */
    public static final String EXAMPLE_SERVO_NAME = "exampleServo";

    // ============================================================
    //                    MOTOR DIRECTIONS
    //  ⚙ TUNE: prop the robot on a block, drive forward, watch the wheels.
    //  All four should spin the same way. Flip whichever ones lied to you.
    // ============================================================

    public static final DcMotorSimple.Direction LEFT_FRONT_DIRECTION  = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction LEFT_BACK_DIRECTION   = DcMotorSimple.Direction.FORWARD;
    public static final DcMotorSimple.Direction RIGHT_FRONT_DIRECTION = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction RIGHT_BACK_DIRECTION  = DcMotorSimple.Direction.FORWARD;

    /** BRAKE = stops dead. FLOAT = coasts like it's on ice. Keep BRAKE. */
    public static final DcMotor.ZeroPowerBehavior DRIVE_ZERO_POWER_BEHAVIOR =
            DcMotor.ZeroPowerBehavior.BRAKE;

    // ============================================================
    //                    DRIVE FEEL
    // ============================================================

    public static final double MAX_DRIVE_POWER = 1.0;
    public static final double MIN_DRIVE_SPEED = 0.2;
    public static final double MAX_DRIVE_SPEED = 1.0;
    public static final double DEFAULT_DRIVE_SPEED = 1.0;

    /** Held right bumper = precision mode, for when "close enough" isn't. */
    public static final double SLOW_MODE_MULTIPLIER = 0.3;

    /** Sticks are never truly centered. Ignore noise below this. */
    public static final double INPUT_THRESHOLD = 0.1;

    /** How close (inches) counts as "arrived" for a path command. */
    public static final double POSE_TOLERANCE = 0.5;

    /** Field-centric: push the stick toward the far wall, robot goes there,
     *  regardless of which way it's facing. Turn this off only if a driver
     *  specifically asks — everyone thinks they want robot-centric until
     *  the robot is pointed at them. */
    public static final boolean DEFAULT_FIELD_CENTRIC = true;

    // ============================================================
    //                    FIELD GEOMETRY
    //  Field is 144" x 144", origin at bottom-left. 0 rad points RIGHT.
    //  Replace these with this year's actual scoring coordinates.
    // ============================================================

    public static final double BLUE_TARGET_X = 12;
    public static final double BLUE_TARGET_Y = 124;
    public static final double RED_TARGET_X  = 132;
    public static final double RED_TARGET_Y  = 124;

    // ============================================================
    //                    PEDRO FOLLOWER
    // ============================================================
    //
    //  ┌───────────────────────────────────────────────────────────────────┐
    //  │  THE TUNING SEQUENCE — DO THESE IN ORDER. ~60–90 MIN TOTAL.       │
    //  │                                                                   │
    //  │  Order matters: every phase assumes the ones above it are done.   │
    //  │  Tuning the PIDs before the velocities means tuning them twice.   │
    //  │                                                                   │
    //  │  FIRST: verify localization (Pinpoint section below) or you'll    │
    //  │  spend an hour tuning PIDs against numbers that were lying.       │
    //  │                                                                   │
    //  │  1. Forward Zero Power Accel   → forwardZeroPowerAcceleration     │
    //  │     stops cleanly, no sliding                          5–10 min   │
    //  │  2. Lateral Zero Power Accel   → lateralZeroPowerAcceleration     │
    //  │     stops cleanly when strafing                        5–10 min   │
    //  │  3. Forward Velocity           → xVelocity (mecanum)              │
    //  │     displayed velocity matches real max speed          5–10 min   │
    //  │  4. Lateral Velocity           → yVelocity (mecanum)              │
    //  │     strafe velocity matches reality                    5–10 min   │
    //  │  5. Translational PID          → translationalPIDFCoefficients    │
    //  │     start with P only; smooth return, no oscillation  10–15 min   │
    //  │  6. Heading PID                → headingPIDFCoefficients          │
    //  │     start with P, maybe add D; no hunting             10–15 min   │
    //  │  7. Drive PID                  → drivePIDFCoefficients            │
    //  │     follows a path without weaving                    10–15 min   │
    //  │  8. Centripetal Force          → centripetalScaling               │
    //  │     cuts corners? swings wide? adjust                 10–15 min   │
    //  │                                                                   │
    //  │  Full walkthrough: docs/tuning.md                                 │
    //  │  Official docs:    https://pedropathing.com/docs/pathing/tuning   │
    //  └───────────────────────────────────────────────────────────────────┘

    public static FollowerConstants followerConstants = new FollowerConstants()
            /** ⚙ TUNE: weigh the robot in kg, battery included. Do this first —
             *  it's the one value you measure instead of tuning. */
            .mass(13.0)

            /** Secondary PIDs are for fine-tuning after everything else works.
             *  Leave false until someone can explain why they're turning it on. */
            .useSecondaryTranslationalPIDF(false)
            .useSecondaryHeadingPIDF(false)
            .useSecondaryDrivePIDF(false)

            /** ⚙ PHASE 1 — Automatic → Forward Zero Power Acceleration */
            // https://pedropathing.com/docs/pathing/tuning/automatic#forward-zero-power-acceleration
            .forwardZeroPowerAcceleration(-45.0)

            /** ⚙ PHASE 2 — Automatic → Lateral Zero Power Acceleration */
            // https://pedropathing.com/docs/pathing/tuning/automatic#lateral-zero-power-acceleration
            .lateralZeroPowerAcceleration(-90.0)

            /** ⚙ PHASE 5 — Manual → Translational. Shove the robot sideways;
             *  it should slide back without arguing with itself. P first. */
            // https://pedropathing.com/docs/pathing/tuning/pids/translational
            .translationalPIDFCoefficients(new PIDFCoefficients(0.08, 0, 0.005, 0.035))

            /** ⚙ PHASE 6 — Manual → Heading. Twist the robot; it should snap
             *  back once, not hunt back and forth. */
            // https://pedropathing.com/docs/pathing/tuning/pids/heading
            .headingPIDFCoefficients(new PIDFCoefficients(0.9, 0, 0.02, 0.03))

            /** ⚙ PHASE 7 — Manual → Drive. Only after the others behave. */
            // https://pedropathing.com/docs/pathing/tuning/pids/drive
            .drivePIDFCoefficients(new FilteredPIDFCoefficients(0.003, 0, 0.0001, 0, 0.3))

            /** ⚙ PHASE 8 — Manual → Centripetal. Range 0.001–0.01.
             *  Cutting inside corners? Raise it. Swinging wide? Lower it. */
            // https://pedropathing.com/docs/pathing/tuning/pids/centripetal
            .centripetalScaling(0.0005);

    // ============================================================
    //                    MECANUM DRIVETRAIN
    //  We run mecanum every single year. This section is the reason
    //  Artemis exists — don't rebuild it, tune it.
    // ============================================================

    public static MecanumConstants mecanumConstants = new MecanumConstants()
            /** ⚙ PHASE 3 — Automatic → Forward Velocity (inches/sec at full power) */
            // https://pedropathing.com/docs/pathing/tuning/automatic#forward-velocity-tuner
            .xVelocity(62.0)
            /** ⚙ PHASE 4 — Automatic → Lateral Velocity. Always lower than
             *  forward: mecanum wheels strafe by scrubbing sideways, which
             *  wastes most of what the motors are offering. */
            // https://pedropathing.com/docs/pathing/tuning/automatic#lateral-velocity-tuner
            .yVelocity(49.0)
            .maxPower(MAX_DRIVE_POWER)
            .leftFrontMotorName(LEFT_FRONT_NAME)
            .leftRearMotorName(LEFT_BACK_NAME)
            .rightFrontMotorName(RIGHT_FRONT_NAME)
            .rightRearMotorName(RIGHT_BACK_NAME)
            .leftFrontMotorDirection(LEFT_FRONT_DIRECTION)
            .leftRearMotorDirection(LEFT_BACK_DIRECTION)
            .rightFrontMotorDirection(RIGHT_FRONT_DIRECTION)
            .rightRearMotorDirection(RIGHT_BACK_DIRECTION);

    /** End T-value, timeout ms, position tolerance (in), heading tolerance (rad). */
    public static PathConstraints pathConstraints = new PathConstraints(0.99, 100, 1, 1);

    // ============================================================
    //                    PINPOINT LOCALIZATION
    // ============================================================
    //
    //  goBILDA Pinpoint: two dead-wheel pods + IMU, fused on its own chip
    //  so the Control Hub doesn't have to think about it.
    //
    //  ┌───────────────────────────────────────────────────────────────────┐
    //  │  VERIFY BEFORE YOU TUNE ANYTHING ELSE. Four tests, by hand:       │
    //  │                                                                   │
    //  │  1. PUSH    — shove the robot forward 12". Does telemetry say     │
    //  │               it moved 12"?                                       │
    //  │  2. ROTATE  — turn it 90° by hand. Heading change ≈ π/2 (1.57)?   │
    //  │  3. STRAFE  — push it sideways. Do X and Y both change the way    │
    //  │               you'd expect?                                       │
    //  │  4. SPIN    — spin it in circles. Does heading wrap cleanly at    │
    //  │               ±π, or does it jump?                                │
    //  │                                                                   │
    //  │  Any test fails → fix it HERE, in the direction/offset settings.  │
    //  │  Tuning PIDs on top of bad localization is tuning against noise.  │
    //  └───────────────────────────────────────────────────────────────────┘

    public static PinpointConstants localizerConstants = new PinpointConstants()
            /** ⚙ TUNE: pod offsets from robot CENTER, in inches. Measure, don't guess. */
            .forwardPodY(3)
            .strafePodX(-9)
            .distanceUnit(DistanceUnit.INCH)
            .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD)
            /** ⚙ TUNE: run Localization Test — pushing forward must INCREASE X.
             *  If it decreases, flip the direction here, not the wiring. */
            // https://pedropathing.com/docs/pathing/tuning/localization/pinpoint#encoder-directions
            .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED)
            .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED);

    // ============================================================
    //                    IMU ORIENTATION
    //  ⚙ TUNE: which way is the Control Hub actually bolted on?
    // ============================================================

    public static final RevHubOrientationOnRobot.LogoFacingDirection IMU_LOGO_DIRECTION =
            RevHubOrientationOnRobot.LogoFacingDirection.UP;
    public static final RevHubOrientationOnRobot.UsbFacingDirection IMU_USB_DIRECTION =
            RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;

    // ============================================================
    //    FOLLOWER FACTORY — where all of the above gets assembled
    // ============================================================

    public static Follower createFollower(HardwareMap hardwareMap) {
        return new FollowerBuilder(followerConstants, hardwareMap)
                .pathConstraints(pathConstraints)
                .mecanumDrivetrain(mecanumConstants)
                .pinpointLocalizer(localizerConstants)
                .build();
    }
}
