package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                              TUNING                                       ║
 * ║                                                                           ║
 * ║  Pedro Pathing 3's AutoTune. There's no Tuning OpMode on the Driver       ║
 * ║  Station any more — the tuners run from a web page:                       ║
 * ║                                                                           ║
 * ║      1. Deploy. Connect a laptop to the robot's Wi-Fi.                    ║
 * ║      2. Open  http://192.168.43.1:10158                                   ║
 * ║      3. Run them top to bottom. Each one ends by handing you code.        ║
 * ║      4. Paste that code into Constants.java and redeploy.                 ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * AutoTune finds every static @Tuner method in the app by itself — nothing
 * else needs registering. The procedures are vendored from Pedro's quickstart;
 * tune the robot, not them. Walkthrough: docs/tuning.md.
 */
public class Tuning {

    /** 1. Which wheel is which, and which way is forward. → Constants.drivetrainConfig */
    @Tuner
    public static Procedure mecanumTuner() {
        return new MecanumTuner();
    }

    /** 2. Pod directions and offsets. → Constants.localizerConfig */
    @Tuner
    public static Procedure pinpointTuner() {
        return new PinpointTuner();
    }

    /** 3. Speed, braking, and every controller. Needs 1 and 2 pasted in first. → Constants.foresightConfig */
    @Tuner
    public static Procedure foresightTuner() {
        return new ForesightTuner(
                hardwareMap -> new PinpointLocalizer(hardwareMap, Constants.localizerConfig),
                hardwareMap -> new Mecanum(hardwareMap, Constants.drivetrainConfig)
        );
    }

    /** 4. Hold, line, curve, and localization checks against everything above. */
    @Tuner
    public static Procedure tests() {
        return new Tests(
                hardwareMap -> new Mecanum(hardwareMap, Constants.drivetrainConfig),
                hardwareMap -> new PinpointLocalizer(hardwareMap, Constants.localizerConfig),
                () -> new Foresight(Constants.foresightConfig)
        );
    }
}
