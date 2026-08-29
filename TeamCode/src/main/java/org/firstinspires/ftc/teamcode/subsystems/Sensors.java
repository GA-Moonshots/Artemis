package org.firstinspires.ftc.teamcode.subsystems;

import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.MyRobot;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                         SENSORS SUBSYSTEM                                 ║
 * ║                                                                           ║
 * ║  Two jobs:                                                                ║
 * ║    • Own every sensor that isn't bolted to a specific mechanism           ║
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

    public Sensors(MyRobot robot) {
        this.robot = robot;
        this.telemetry = robot.telemetry;
        this.megaphone = PanelsTelemetry.INSTANCE.getTelemetry();

        // Add this year's sensors here — color sensor, Limelight, distance
        // sensors. Wrap anything that might not be plugged in in a try/catch
        // and set it null; a missing sensor should degrade the robot, not
        // brick it thirty seconds before a match.
    }

    /** Queue one line. Prints nothing until periodic() flushes. */
    public void addTelemetry(String key, String value) {
        megaphone.addData(key, value);
    }

    /** Same, but with String.format built in: addTelemetry("X", "%.1f\"", x) */
    public void addTelemetry(String key, String format, Object... args) {
        megaphone.addData(key, String.format(format, args));
    }

    @Override
    public void periodic() {
        // The one flush. Sends to Panels and the Driver Station together.
        megaphone.update(telemetry);
    }
}
