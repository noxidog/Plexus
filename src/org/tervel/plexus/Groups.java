package org.tervel.plexus;

import java.util.ArrayList;
import java.util.List;

/**
 * The three symmetry vocabularies the router uses, as cell maps about the origin, and the order each
 * symmetry walk asks its questions in.
 *
 * <ul>
 *   <li><b>D4</b> on square cells {@code (row, column)}: identity, rot90, rot180, rot270, mirrorH (rows flip),
 *       mirrorV (columns flip), diagMain, diagAnti.</li>
 *   <li><b>D2</b>, used on a rectangle after enveloping it to landscape: identity, rot180, the short-axis
 *       mirror (columns flip: it swaps the two ends of the long side), the long-axis mirror (rows flip).</li>
 *   <li><b>D6</b> on hex cells in axial {@code (q, r)}: the six turns and six mirrors, generated from the 60°
 *       turn {@code (q, r) -> (-r, q + r)} and the mirror {@code (q, r) -> (r, q)}, and named by what they do in
 *       the plane ({@code rotN}, {@code mirrorN} with the axis at N degrees).</li>
 * </ul>
 *
 * <p>The walk orders put turns first, smallest angle first, then mirrors: short-axis then long-axis on a
 * rectangle; mirrorH, mirrorV, diagMain, diagAnti on a square box; by axis angle on hex. A turn whose inverse is
 * already asked (rot270 on D4, rot240 and rot300 on D6) is left out: it always gets the same answer.
 */
final class Groups {

    private Groups() { }

    static final List<CellSet.Map> D4 = List.of(
            new CellSet.Map("identity", (a, b) -> a, (a, b) -> b),
            new CellSet.Map("rot90", (a, b) -> b, (a, b) -> -a),
            new CellSet.Map("rot180", (a, b) -> -a, (a, b) -> -b),
            new CellSet.Map("rot270", (a, b) -> -b, (a, b) -> a),
            new CellSet.Map("mirrorH", (a, b) -> -a, (a, b) -> b),
            new CellSet.Map("mirrorV", (a, b) -> a, (a, b) -> -b),
            new CellSet.Map("diagMain", (a, b) -> b, (a, b) -> a),
            new CellSet.Map("diagAnti", (a, b) -> -b, (a, b) -> -a));

    /** D2 on a landscape rectangle, in pose-code order. */
    static final List<CellSet.Map> D2 = List.of(
            D4.get(0),
            D4.get(2),
            new CellSet.Map("short-axis mirror", D4.get(5).fa(), D4.get(5).fb()),
            new CellSet.Map("long-axis mirror", D4.get(4).fa(), D4.get(4).fb()));

    static final List<CellSet.Map> D6 = buildD6();

    /** The walk questions for each kind of shape, as maps from the kind's own vocabulary. */
    static final List<CellSet.Map> WALK_RECT = List.of(D2.get(1), D2.get(2), D2.get(3));
    static final List<CellSet.Map> WALK_SQUARE = List.of(D4.get(1), D4.get(2), D4.get(4), D4.get(5), D4.get(6), D4.get(7));
    static final List<CellSet.Map> WALK_HEX = byNames(D6, "rot60", "rot120", "rot180",
            "mirror0", "mirror30", "mirror60", "mirror90", "mirror120", "mirror150");


    static List<CellSet.Map> walkFor(Kind kind) {
        return switch (kind) {
            case RECT -> WALK_RECT;
            case SQUARE -> WALK_SQUARE;
            case HEX -> WALK_HEX;
            case EMPTY -> List.of();
        };
    }

    /** How many transforms fix {@code s}. */
    static int stabilizerSize(CellSet s, List<CellSet.Map> group) {
        var n = 0;
        for (final var g : group) if (s.map(g).equals(s)) n++;
        return n;
    }

    /** The smallest image of {@code s} over the group: its form up to the group, the same for every pose. */
    static CellSet orbitMin(CellSet s, List<CellSet.Map> group) {
        CellSet best = null;
        for (final var g : group) {
            final var img = s.map(g);
            if (best == null || img.compareTo(best) < 0) best = img;
        }
        return best;
    }

    private static List<CellSet.Map> byNames(List<CellSet.Map> group, String... names) {
        final var out = new ArrayList<CellSet.Map>();
        for (final var n : names)
            out.add(group.stream().filter(g -> g.name().equals(n)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("no " + n + " in D6")));
        return List.copyOf(out);
    }

    private static List<CellSet.Map> buildD6() {
        final var out = new ArrayList<CellSet.Map>();
        for (var mirrored = 0; mirrored < 2; mirrored++)
            for (var k = 0; k < 6; k++) {
                final int turns = k;
                final boolean m = mirrored == 1;
                final java.util.function.IntBinaryOperator fa = (q, r) -> applyHex(q, r, turns, m)[0];
                final java.util.function.IntBinaryOperator fb = (q, r) -> applyHex(q, r, turns, m)[1];
                final var img1 = applyHex(1, 0, turns, m);
                final var img2 = applyHex(0, 1, turns, m);
                final var det = img1[0] * img2[1] - img2[0] * img1[1];
                final var x = img1[0] + 0.5 * img1[1];
                final var y = Math.sqrt(3) / 2 * img1[1];
                final var angle = Math.toDegrees(Math.atan2(y, x));
                final String name;
                if (!m && k == 0) name = "identity";
                else if (det > 0) name = "rot" + (((Math.round(angle) % 360) + 360) % 360);
                else name = "mirror" + (((Math.round(angle / 2) % 180) + 180) % 180);
                out.add(new CellSet.Map(name, fa, fb));
            }
        return List.copyOf(out);
    }

    /** Apply the mirror (if any) then {@code turns} 60° turns to {@code (q, r)}. */
    private static int[] applyHex(int q, int r, int turns, boolean mirror) {
        int a = q, b = r;
        if (mirror) { final var t = a; a = b; b = t; }
        for (var i = 0; i < turns; i++) { final var na = -b; final var nb = a + b; a = na; b = nb; }
        return new int[]{a, b};
    }
}
