package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.invariants.Position;
import org.tervel.plexus.ops.Searcher;
import org.tervel.plexus.ops.Topology;
import org.tervel.plexus.symmetry.SymmetryGroup;
import org.tervel.plexus.symmetry.Transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Query by stabilizer. Two modes, both reached from menu {@code [1]}:
 *
 * <ul>
 *   <li><b>posed</b> — specify a <b>canonical shape + allowed symmetries</b>. The symmetries name the
 *       stabilizer to query under <em>and</em> orient the canonical shape, then the consumer answers the
 *       single-axis terminal question: does each symmetry survive or break the label?</li>
 *   <li><b>solve</b> — specify a <b>canonical shape + a label</b> (blank symmetries) and a direction. The
 *       stabilizer is solved from the label, on the axis that actually routes the shape:
 *       <ul>
 *         <li><b>oriented shapes</b> climb the <b>D4 subgroup lattice</b>. Walking the orbit against the raw
 *             dataset labels splits the group into <em>surviving</em> transforms (image keeps the label) and
 *             <em>broken</em> ones (image flips it); <b>gather</b> climbs to the largest label-pure subgroup
 *             (version-space general boundary — maximal recall), <b>separate</b> drops to the shape's own
 *             geometric stabilizer (specific boundary). Reported directly, no pose step.</li>
 *         <li><b>radial shapes</b> (the circular group — dots, plus) have no orientation, so symmetry says
 *             nothing; they route by <b>Position</b>. Solve therefore walks the <b>position lattice</b>: it
 *             slides the shape over every placement and splits the <em>quadrants</em> into surviving / broken
 *             by the dataset labels. <b>gather</b> is the union of label-pure positions (the geometric
 *             shape-fiber — recall the shape wherever it keeps the label); <b>separate</b> pins to the
 *             shape's own quadrant. This is where the dot-position contradictions live.</li>
 *       </ul></li>
 * </ul>
 */
public class StabilizerQuery implements Report {

    private static final String[] QUADRANT =
            {"top-left", "top", "top-right", "left", "centre", "right", "bottom-left", "bottom", "bottom-right"};

    /** Solve-mode configuration: the wanted label, the direction, and the dataset (grids + labels). */
    private record Solve(int label, boolean gather, List<int[]> grids, List<Integer> labels) { }

    private final int[] grid;
    private final int allowedMask;
    private final Solve solve;

    /** Posed mode: query under an explicit stabilizer mask. */
    public StabilizerQuery(int[] grid, int allowedMask) {
        this.grid = grid.clone();
        this.allowedMask = allowedMask;
        this.solve = null;
    }

    /** Solve mode: solve the stabilizer from a label ({@code gather} = expand, else separate = shrink). */
    public StabilizerQuery(int[] grid, int label, boolean gather, List<int[]> grids, List<Integer> labels) {
        this.grid = grid.clone();
        this.allowedMask = 0;
        this.solve = new Solve(label, gather, grids, labels);
    }

    @Override
    public String apply(Plexus plexus) {
        if (solve == null) return posed(plexus);
        final var centered = Plexus.center(grid);
        if (centered == null) return "empty shape — nothing to solve";
        return plexus.descriptor(centered).radial()
                ? solvedRadial(plexus, centered)       // circular group: the position axis
                : solvedOriented(plexus, centered);    // oriented shapes: the D4 subgroup lattice
    }

    // ---- solve mode, oriented: derive the stabilizer from the label on the D4 lattice ---------

    private String solvedOriented(Plexus plexus, int[] centered) {
        final var group = plexus.group();
        final var ts = group.transforms();
        final var side = (int) Math.round(Math.sqrt(centered.length));
        // Deliberately read RAW dataset labels here, not the Plexus's verdict. Everywhere else we consult
        // the Plexus (the existing body of knowledge — see posed()); but its context answer is the
        // *majority*, which would hide the very planted contradictions solve exists to expose. So this one
        // decision goes to ground truth instead of the trained router.
        final var byCrop = crops(solve.grids(), solve.labels());

        // Split the orbit by the raw dataset label of each image (matched translation-free, by crop).
        var surviving = 0; var broken = 0; var unknown = 0;
        for (var k = 0; k < ts.size(); k++) {
            final var labels = byCrop.get(cropKey(ts.get(k).apply(centered)));
            if (labels == null || labels.isEmpty()) unknown |= 1 << k;
            else if (labels.size() == 1 && labels.contains(solve.label())) surviving |= 1 << k;
            else broken |= 1 << k;
        }

        final var mask = solve.gather()
                ? largestSubgroupWithin(group, side, surviving)   // general boundary: max label-pure fold
                : group.stabilizer(centered);                     // specific boundary: the shape's own

        final var sb = new StringBuilder();
        header(sb, "", side, centered);
        if ((surviving & 1) == 0)
            sb.append("  note: the input itself is not label ").append(solve.label())
              .append(" in the data (the still-path is ").append((broken & 1) != 0 ? "forbidden" : "unseen").append(")\n");
        sb.append("  allowed paths   (stays the target): ").append(group.describe(surviving)).append('\n');
        sb.append("  forbidden paths (label dies):       ").append(group.describe(broken)).append('\n');
        if (unknown != 0)
            sb.append("  unseen paths    (never in data):    ").append(group.describe(unknown)).append('\n');
        sb.append("  => history class [").append(group.describe(mask)).append("]  — the paths it sums over (")
          .append(solve.gather() ? "largest label-pure orbit" : "the input's own stabilizer").append(")\n");
        if (solve.gather()) {
            final var shapes = new LinkedHashSet<String>();
            for (var k = 0; k < ts.size(); k++) if ((mask & (1 << k)) != 0) shapes.add(cropKey(ts.get(k).apply(centered)));
            sb.append("  sums over ").append(shapes.size()).append(" distinct shape(s) as one history.\n");
        }
        return sb.toString();
    }

    // ---- solve mode, radial: derive a position-class from the label on the position lattice ----

    private String solvedRadial(Plexus plexus, int[] centered) {
        final var side = (int) Math.round(Math.sqrt(centered.length));
        final var p = patch(centered);
        final var pos = new Position();
        // Read raw per-placement labels rather than consult the Plexus, on purpose: the default invariant
        // chain carries no Position, so the Plexus folds every dot placement into one radial context — the
        // exact collapse this branch exists to undo. Consult the body of knowledge only where it actually
        // distinguishes the axis; here it cannot, so we go to ground truth keyed by exact placement.
        final var byGrid = exact(solve.grids(), solve.labels());

        // Slide the shape over every placement; aggregate the dataset labels by centre-of-mass quadrant.
        final var byQuadrant = new HashMap<Integer, Set<Integer>>();
        for (var top = 0; top + p.h <= side; top++)
            for (var left = 0; left + p.w <= side; left++) {
                final var g = embed(p.cells, p.h, p.w, side, top, left);
                final var labels = byGrid.get(Arrays.toString(g));
                if (labels != null && !labels.isEmpty())
                    byQuadrant.computeIfAbsent(pos.applyAsInt(g), k -> new HashSet<>()).addAll(labels);
            }

        final var surviving = new ArrayList<Integer>();
        final var broken = new ArrayList<Integer>();
        for (var q = 0; q < QUADRANT.length; q++) {
            final var labels = byQuadrant.get(q);
            if (labels == null || labels.isEmpty()) continue;
            (labels.size() == 1 && labels.contains(solve.label()) ? surviving : broken).add(q);
        }
        final var seedQuadrant = pos.applyAsInt(centered);

        final var sb = new StringBuilder();
        header(sb, "  (radial — the paths are positions, not symmetries)", side, centered);
        sb.append("  allowed paths   (position stays target): ").append(quadrants(surviving)).append('\n');
        sb.append("  forbidden paths (position dies):         ").append(quadrants(broken)).append('\n');
        if (solve.gather())
            sb.append("  => sums over ").append(surviving.size()).append(" allowed position(s) as one history: ")
              .append(quadrants(surviving)).append('\n');
        else
            sb.append("  => the input's own path — pinned to [").append(QUADRANT[seedQuadrant]).append("]\n");
        return sb.toString();
    }

    private void header(StringBuilder sb, String suffix, int side, int[] g) {
        sb.append("sum-over-paths — ")
          .append(solve.gather() ? "gather: which moves keep the input alive?" : "separate: the input's own path")
          .append("  (label=").append(solve.label()).append(")").append(suffix).append('\n');
        for (var r = 0; r < side; r++) {
            sb.append("    ");
            for (var c = 0; c < side; c++) sb.append(g[r * side + c] == 1 ? 'x' : '.');
            sb.append('\n');
        }
    }

    private static String quadrants(List<Integer> qs) {
        if (qs.isEmpty()) return "(none)";
        final var out = new ArrayList<String>();
        qs.forEach(q -> out.add(QUADRANT[q]));
        return String.join(", ", out);
    }

    // ---- dataset label lookups ----------------------------------------------------------------

    /** Dataset labels keyed translation-free, by crop — for the oriented (symmetry) branch. */
    private static Map<String, Set<Integer>> crops(List<int[]> grids, List<Integer> labels) {
        final var map = new HashMap<String, Set<Integer>>();
        for (var i = 0; i < grids.size(); i++)
            map.computeIfAbsent(cropKey(grids.get(i)), k -> new HashSet<>()).add(labels.get(i));
        return map;
    }

    /** Dataset labels keyed by exact placement — for the radial (position) branch. */
    private static Map<String, Set<Integer>> exact(List<int[]> grids, List<Integer> labels) {
        final var map = new HashMap<String, Set<Integer>>();
        for (var i = 0; i < grids.size(); i++)
            map.computeIfAbsent(Arrays.toString(grids.get(i)), k -> new HashSet<>()).add(labels.get(i));
        return map;
    }

    /** A translation-free key for a grid's active cells: {@code "HxW:bits"} over the bounding box. */
    private static String cropKey(int[] g) {
        final var p = patch(g);
        if (p.h == 0) return "empty";
        final var sb = new StringBuilder().append(p.h).append('x').append(p.w).append(':');
        for (final var cell : p.cells) sb.append(cell == 1 ? '1' : '0');
        return sb.toString();
    }

    // ---- bounding-box patch + placement -------------------------------------------------------

    private record Patch(int[] cells, int h, int w) { }

    /** Crop a grid to its bounding-box pattern; {@code (empty,0,0)} if blank. */
    private static Patch patch(int[] g) {
        final var side = (int) Math.round(Math.sqrt(g.length));
        var minR = side; var maxR = -1; var minC = side; var maxC = -1;
        for (var i = 0; i < g.length; i++) if (g[i] == 1) {
            final int r = i / side, c = i % side;
            minR = Math.min(minR, r); maxR = Math.max(maxR, r);
            minC = Math.min(minC, c); maxC = Math.max(maxC, c);
        }
        if (maxR < 0) return new Patch(new int[0], 0, 0);
        final var h = maxR - minR + 1; final var w = maxC - minC + 1;
        final var cells = new int[h * w];
        for (var r = 0; r < h; r++) for (var c = 0; c < w; c++)
            cells[r * w + c] = g[(minR + r) * side + (minC + c)];
        return new Patch(cells, h, w);
    }

    /** Place a {@code h×w} pattern into a {@code side×side} grid with top-left corner at (top,left). */
    private static int[] embed(int[] cells, int h, int w, int side, int top, int left) {
        final var g = new int[side * side];
        for (var r = 0; r < h; r++) for (var c = 0; c < w; c++)
            if (cells[r * w + c] == 1) g[(top + r) * side + (left + c)] = 1;
        return g;
    }

    // ---- subgroup search (oriented branch) ----------------------------------------------------

    /**
     * The largest subgroup whose every element is set in {@code mask} (identity is index 0, always in).
     *
     * <p><b>Exhaustive, deliberately NOT greedy.</b> The tempting greedy move — "keep every surviving
     * transform" — is wrong: a set of label-pure transforms need not be a subgroup (two surviving mirrors
     * can compose to a rotation that is broken), so greedily unioning them overshoots into a non-group. We
     * instead test every closed subset of {@code mask} and take the largest, so the result is always a true
     * subgroup. The space is tiny (2^|G|), so exhaustive costs nothing and buys correctness.
     *
     * <p><b>When the surviving set is not itself a subgroup,</b> this does NOT fall back to a loose set of
     * generators (an invalid fold). It returns the <em>largest subgroup wholly contained in</em> the
     * surviving set — the biggest closed subset that lies inside {@code mask} and contains the identity. So
     * recall is capped at the largest valid fold the labels permit, and in the worst case (no non-trivial
     * surviving transform composes back inside the set) it floors at the trivial group {@code {e}} —
     * identity alone, which {@code best = 1} seeds and which is always a subgroup.
     */
    private static int largestSubgroupWithin(SymmetryGroup group, int side, int mask) {
        final var n = group.transforms().size();
        final var cayley = cayley(group, side);
        var best = 1;                                              // identity alone is always a subgroup
        for (var sub = 1; sub < (1 << n); sub++) {
            if ((sub & mask) != sub || (sub & 1) == 0) continue;  // must be ⊆ mask and contain identity
            if (Integer.bitCount(sub) <= Integer.bitCount(best)) continue;
            if (closed(sub, cayley, n)) best = sub;
        }
        return best;
    }

    /** Whether {@code sub} is closed under composition — given identity, that makes it a subgroup. */
    private static boolean closed(int sub, int[][] cayley, int n) {
        for (var a = 0; a < n; a++) if ((sub & (1 << a)) != 0)
            for (var b = 0; b < n; b++) if ((sub & (1 << b)) != 0)
                if ((sub & (1 << cayley[a][b])) == 0) return false;
        return true;
    }

    /** The composition table: {@code cayley[a][b]} is the index of {@code a ∘ b}, found via a probe grid. */
    private static int[][] cayley(SymmetryGroup group, int side) {
        final var ts = group.transforms();
        final var n = ts.size();
        final var probe = new int[side * side];
        for (var i = 0; i < probe.length; i++) probe[i] = i;      // distinct cells ⇒ each map is identifiable
        final var images = new int[n][];
        for (var c = 0; c < n; c++) images[c] = ts.get(c).apply(probe);
        final var tab = new int[n][n];
        for (var a = 0; a < n; a++)
            for (var b = 0; b < n; b++) {
                final var comp = ts.get(a).apply(ts.get(b).apply(probe));
                for (var c = 0; c < n; c++) if (Arrays.equals(images[c], comp)) { tab[a][b] = c; break; }
            }
        return tab;
    }

    // ---- posed mode: query under an explicit stabilizer ---------------------------------------

    private String posed(Plexus plexus) {
        final var group = plexus.group();
        final var centered = Plexus.center(grid);
        if (centered == null) return "empty shape — nothing to pose";

        // Pose: find the orientation of the shape whose stabilizer is exactly the allowed symmetries.
        // Greedy by design — the FIRST matching orientation wins. Orientations sharing a stabilizer route
        // to the same context, so any of them answers the query identically; no need to enumerate the rest.
        int[] posed = null;
        for (final var t : group.transforms()) {
            final var img = Plexus.center(t.apply(centered));
            if (group.stabilizer(img) == allowedMask) { posed = img; break; }
        }
        if (posed == null)
            return "no orientation of this shape has stabilizer [" + group.describe(allowedMask) + "].\n"
                    + "achievable stabilizers: " + achievable(group, centered);

        // Consult the Plexus — its descriptor, context, and score are the trained body of knowledge.
        // We read the answer it already computed rather than re-derive it here.
        final var key = plexus.descriptor(posed);
        final var ctx = plexus.context(posed);
        final var ans = plexus.score(posed);
        final var side = (int) Math.round(Math.sqrt(posed.length));

        final var sb = new StringBuilder();
        sb.append("posed shape — stabilizer [").append(group.describe(allowedMask)).append("]:\n");
        for (var r = 0; r < side; r++) {
            sb.append("    ");
            for (var c = 0; c < side; c++) sb.append(posed[r * side + c] == 1 ? 'x' : '.');
            sb.append('\n');
        }
        sb.append("  break:  ").append(plexus.coordString(key)).append('\n');
        if (ctx == null) {
            sb.append("  label:  unseen break -> reject (0)\n");
        } else {
            sb.append(String.format("  label:  answer=%d (%s)  pos=%d neg=%d%s%n",
                    ctx.answer(), ctx.isTarget() ? "TARGET" : "reject", ctx.pos(), ctx.neg(),
                    ctx.isException(posed) ? "  [this grid is an exception -> score " + ans + "]" : ""));
        }

        sb.append("  which moves keep the input on its orbit?\n");
        sb.append("    allowed paths   (keep ").append(ans).append("): ").append(names(new Searcher(posed, ans).apply(plexus))).append('\n');
        sb.append("    forbidden paths (flip):    ").append(names(new Searcher(posed, 1 - ans).apply(plexus))).append('\n');

        // The sibling breaks: other shapes valid under the same stabilizer (fix stabilizer, vary break).
        final var siblings = new ArrayList<String>();
        new Topology(node -> {
            if (node.radial() != key.radial() || node.key().symmetry() != key.symmetry()) return;
            siblings.add("    |- " + node.coords() + (node.coords().equals(plexus.coordString(key)) ? "  <- this shape" : ""));
        }).apply(plexus);
        sb.append("  sibling breaks under [").append(group.describe(allowedMask)).append("] (").append(siblings.size()).append("):\n");
        siblings.forEach(s -> sb.append(s).append('\n'));
        return sb.toString();
    }

    /** The distinct stabilizers reachable across the shape's orientations — what you may legally ask for. */
    private static String achievable(SymmetryGroup group, int[] centered) {
        final var set = new TreeSet<String>();
        for (final var t : group.transforms())
            set.add("[" + group.describe(group.stabilizer(Plexus.center(t.apply(centered)))) + "]");
        return String.join("  ", set);
    }

    private static String names(Iterator<Transform> it) {
        final var out = new ArrayList<String>();
        it.forEachRemaining(t -> out.add(t.label()));
        return out.isEmpty() ? "(none)" : String.join(", ", out);
    }
}
