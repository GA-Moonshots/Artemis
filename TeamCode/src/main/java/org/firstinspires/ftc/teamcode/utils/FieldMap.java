package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.geometry.Pose;
import com.qualcomm.hardware.limelightvision.LLFieldMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

import java.util.ArrayList;
import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                            FIELD MAP                                      ║
 * ║                                                                           ║
 * ║  Where the AprilTags are, and how to translate between everybody's        ║
 * ║  idea of "where".                                                         ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * WE DO NOT TYPE IN TAG COORDINATES. FIRST ships them in the SDK, they're
 * correct, and they change with the game. We read
 * AprilTagGameDatabase.getCurrentGameTagLibrary() and convert. Next season the
 * SDK updates and this file keeps working — which is the entire reason it
 * reads from the library instead of a hand-maintained table.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  NOT EVERY TAG IS A LOCALIZATION TAG
 * ─────────────────────────────────────────────────────────────────────────────
 * Some tags mark a known spot on the field. Others just encode information —
 * which pattern to score, which side to start on — and are mounted somewhere
 * the game manual never pins down. Localize off one of those and your pose is
 * fiction.
 *
 * FIRST distinguishes them for us: informational tags ship with NO field
 * position. So "has a field position" is the filter, and it needs no
 * per-season ID blacklist that somebody will forget to update.
 *
 * The 2025-26 DECODE field, as a worked example: tags 20 and 24 were the goal
 * targets and carried positions; 21/22/23 were Obelisk motif tags and did not.
 * Expect entirely different ids, positions, sizes, and counts this season —
 * nothing below assumes otherwise, and nothing below should be edited when the
 * game changes.
 *
 * How many localization tags a field has matters for the trust policy: DECODE
 * had only two, at opposite ends, so "require several tags at once" would
 * essentially never fire. A field with tags on every wall could afford a
 * stricter rule. See Vision — the policy reads tag count as a signal rather
 * than assuming a number.
 *
 * See Vision for how a sighting becomes a pose correction, and
 * docs/coordinates.md for the frame rules.
 */
public class FieldMap {

    /** Half the field, in inches. FTC's frame is centred; ours isn't. */
    public static final double HALF_FIELD_INCHES = 72.0;

    public static final double METERS_TO_INCHES = 39.3701;

    /** One localization tag, already converted into our frame. */
    public static class Tag {
        public final int id;
        public final String name;
        /** Pedro frame: inches, corner origin. */
        public final double x, y, z;
        /** Edge length in inches. Varies by game — never hardcode it. */
        public final double sizeInches;

        Tag(int id, String name, double x, double y, double z, double sizeInches) {
            this.id = id; this.name = name;
            this.x = x; this.y = y; this.z = z;
            this.sizeInches = sizeInches;
        }
    }

    // ============================================================
    //                    UNIT / FRAME CONVERSIONS
    //   Convert once, at the edge. A conversion buried in the middle
    //   of a command is a bug waiting for a Saturday.
    // ============================================================

    public static double metersToInches(double meters) {
        return meters * METERS_TO_INCHES;
    }

    /**
     * FTC field coordinates → Pedro's.
     *
     * FTC puts the origin at the field CENTRE (so coordinates run -72..+72).
     * Pedro puts it at a CORNER (0..144). The shift is +72 on both axes.
     *
     * ⚠ VERIFY THIS ON THE FIELD. The +72 shift is certain. Whether the two
     * frames also disagree about which way X and Y point is NOT something you
     * can settle by reading code — the Panels Pedro preset applies a 90°
     * rotation, which hints the axes may not line up. Run CameraCalibration,
     * park at a known spot, and see whether the reported pose matches. If X
     * and Y come back swapped or mirrored, fix it HERE, in this one method,
     * and nowhere else.
     */
    public static double[] ftcToPedro(double ftcX, double ftcY) {
        return new double[] { ftcX + HALF_FIELD_INCHES, ftcY + HALF_FIELD_INCHES };
    }

    public static double[] pedroToFtc(double pedroX, double pedroY) {
        return new double[] { pedroX - HALF_FIELD_INCHES, pedroY - HALF_FIELD_INCHES };
    }

    /**
     * A Limelight botpose → a Pedro Pose.
     *
     * The Limelight reports metres, degrees, and (with the stock field map)
     * a centre origin. So: metres→inches, centre→corner, degrees→radians.
     */
    public static Pose limelightToPedro(Pose3D botpose) {
        double xIn = metersToInches(botpose.getPosition().x);
        double yIn = metersToInches(botpose.getPosition().y);
        double[] pedro = ftcToPedro(xIn, yIn);
        double headingRad = Math.toRadians(botpose.getOrientation().getYaw());
        return new Pose(pedro[0], pedro[1], headingRad);
    }

    // ============================================================
    //                    THE TAGS
    // ============================================================

    /**
     * Every tag this season that we can actually localize from, in our frame.
     * Tags without a field position (Obelisk motif tags) are filtered out.
     */
    public static List<Tag> localizationTags() {
        List<Tag> tags = new ArrayList<>();
        try {
            AprilTagLibrary library = AprilTagGameDatabase.getCurrentGameTagLibrary();
            for (AprilTagMetadata meta : library.getAllTags()) {
                if (meta.fieldPosition == null) continue;   // motif tag, not a landmark

                // The library reports in its own DistanceUnit; normalise to inches.
                double xIn = meta.distanceUnit.toInches(meta.fieldPosition.get(0));
                double yIn = meta.distanceUnit.toInches(meta.fieldPosition.get(1));
                double zIn = meta.distanceUnit.toInches(meta.fieldPosition.get(2));

                double[] pedro = ftcToPedro(xIn, yIn);
                double sizeIn = meta.distanceUnit.toInches(meta.tagsize);
                tags.add(new Tag(meta.id, meta.name, pedro[0], pedro[1], zIn, sizeIn));
            }
        } catch (Exception e) {
            // A missing tag library shouldn't stop the robot driving.
        }
        return tags;
    }

    /** @return the tag with this id, or null if it isn't a localization tag. */
    public static Tag byId(int id) {
        for (Tag t : localizationTags()) {
            if (t.id == id) return t;
        }
        return null;
    }

    // ============================================================
    //                 UPLOADING TO THE LIMELIGHT
    // ============================================================

    /**
     * Builds a Limelight field map from the SDK's tag data.
     *
     * Why bother, when the Limelight has its own map? Because a map that lives
     * only on the camera is one reflash away from gone, and nobody remembers
     * what was in it. This one lives in git, updates itself when the SDK
     * updates, and gets pushed at init.
     *
     * ⚠ UNVERIFIED ON HARDWARE. The Limelight expects a 4x4 row-major
     * transform in METRES. The translation below is right; the rotation is
     * left as identity, which is fine for position-only localization but means
     * tag FACING is not described. If MegaTag results look wrong in a way that
     * smells like orientation, this is the first place to look — and write down
     * what you find in docs/issue-log.md.
     */
    public static LLFieldMap buildLimelightFieldMap() {
        List<LLFieldMap.Fiducial> fiducials = new ArrayList<>();

        for (Tag tag : localizationTags()) {
            double[] ftc = pedroToFtc(tag.x, tag.y);
            double xM = ftc[0] / METERS_TO_INCHES;
            double yM = ftc[1] / METERS_TO_INCHES;
            double zM = tag.z / METERS_TO_INCHES;

            // 4x4 row-major, identity rotation + translation in the last column.
            List<Double> transform = new ArrayList<>();
            double[] m = {
                    1, 0, 0, xM,
                    0, 1, 0, yM,
                    0, 0, 1, zM,
                    0, 0, 0, 1
            };
            for (double v : m) transform.add(v);

            fiducials.add(new LLFieldMap.Fiducial(
                    tag.id,
                    DistanceUnit.INCH.toMeters(tag.sizeInches),   // from the library, not hardcoded
                    "apriltag3_36h11_classic",
                    transform,
                    true));
        }

        return new LLFieldMap(fiducials, "frc");
    }
}
