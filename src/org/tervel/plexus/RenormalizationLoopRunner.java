package org.tervel.plexus;

import org.tervel.plexus.diagnostics.CollisionDiagnostic;
import org.tervel.plexus.invariants.Connectivity;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.invariants.Mass;
import org.tervel.plexus.invariants.Signature;
import org.tervel.plexus.ops.Scale;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.symmetry.Dihedral;
import org.tervel.plexus.symmetry.SymmetryGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The <b>Two-Scale Renormalization Closure Loop</b> (§11, the "Change of scale" TODO), run as a standalone
 * experiment. It tests whether a structural law survives a change of scale by coarse-graining a fine
 * dataset with the existing {@link Scale} fold, retraining a fresh {@link Plexus} on the result, and
 * comparing what each scale's {@link CollisionDiagnostic} demands — expressed natively in Plexus's own
 * exception terms:
 *
 * <ul>
 *   <li><b>BLUEPRINT</b> — the set of {@code Primitive} names the diagnostic emits for SEPARABLE collisions:
 *       <em>reducible</em> exceptions a richer invariant chain could absorb (counterterms).</li>
 *   <li><b>FLOOR</b> — the count of PROVABLE-TWIN collisions: the <em>irreducible</em> exceptions that land
 *       permanently in the exact-exception store. This is the quantity renormalization must preserve.</li>
 * </ul>
 *
 * <h2>The dataset (path vs. fork, in exception terms)</h2>
 * Fine resolution {@code side = 9}, decimation {@code k = 3} ({@code k | side}), coarse {@code side/k = 3}.
 * Two D4-orbit classes that share the <b>full routing key</b> at fine scale — same canonical stabilizer
 * (trivial, off-centre placements) and same {@link Signature} {@code (V=4, E=3)} — so they land in <b>one
 * entropy&gt;0 context</b>, yet lie in <b>disjoint orbits</b> (no rigid map carries a path onto a fork):
 *
 * <ul>
 *   <li>label 1 (TARGET): the <b>T-tetromino</b> — a branched tree, {@code Δ = 3} (a fork).</li>
 *   <li>label 0: the <b>L-tetromino</b> — a simple path, {@code Δ = 2}, same {@code (V, E)}.</li>
 * </ul>
 *
 * So the fine collision is SEPARABLE (blueprint {@code {max-degree}}, floor 0): its minority is memorised
 * as <em>reducible</em> exceptions the {@code MaxDegree} axis would dissolve. The loop then asks whether the
 * {@code k = 3} fold keeps them separable (closure / soft growth) or collapses an oppositely-labelled path
 * and fork onto one coarse canonical shape — turning reducible exceptions into <b>twins</b> and raising the
 * irreducible floor (hard shattering).
 */
public final class RenormalizationLoopRunner {

    private static final int FINE_SIDE = 9;
    private static final int K = 3;

    public static void main(String[] args) {
        final SymmetryGroup group = Dihedral.d4();

        // ---- 1. multi-scale dataset (synthesised in-code) -----------------------------------------
        final var grids = new ArrayList<int[]>();
        final var labels = new ArrayList<Integer>();
        final int[][] tBase = {{0, 0}, {0, 1}, {0, 2}, {1, 1}};   // T-tetromino: fork, Δ=3
        final int[][] lBase = {{0, 0}, {1, 0}, {2, 0}, {2, 1}};   // L-tetromino: path, Δ=2
        placeAll(grids, labels, orientations(tBase), FINE_SIDE, 1);
        placeAll(grids, labels, orientations(lBase), FINE_SIDE, 0);

        // ---- 1. fine baseline ---------------------------------------------------------------------
        final var plexus9 = new Plexus(group, chain(FINE_SIDE), new ExactExceptions());
        plexus9.fit(grids, labels);
        final var fine = new CollisionDiagnostic(grids, labels).analyze(plexus9);

        // ---- 2. scale fold (9x9 -> 3x3), carrying labels, summing integrated entropy --------------
        final var coarse = new ArrayList<int[]>();
        final var coarseLabels = new ArrayList<Integer>();
        var integratedEntropy = 0.0;
        for (var i = 0; i < grids.size(); i++) {
            final var fold = new Scale(grids.get(i), K, Scale.Rule.CANONICAL).apply(plexus9);
            coarse.add(fold.coarse());
            coarseLabels.add(labels.get(i));
            integratedEntropy += fold.integratedEntropy();
        }
        final var coarseSide = FINE_SIDE / K;

        // ---- 3. coarse evaluation (fresh Plexus, same minimal chain) ------------------------------
        final var plexus3 = new Plexus(group, chain(coarseSide), new ExactExceptions());
        plexus3.fit(coarse, coarseLabels);
        final var coarseResult = new CollisionDiagnostic(coarse, coarseLabels).analyze(plexus3);

        // ---- report -------------------------------------------------------------------------------
        System.out.println("=== Two-Scale Renormalization Closure Loop ===");
        System.out.printf("dataset: %d fine %dx%d grids (%d T / %d L), decimation k=%d -> %dx%d%n",
                grids.size(), FINE_SIDE, FINE_SIDE,
                labels.stream().filter(l -> l == 1).count(), labels.stream().filter(l -> l == 0).count(),
                K, coarseSide, coarseSide);
        System.out.println();
        report("FINE  (" + FINE_SIDE + "x" + FINE_SIDE + ")", plexus9, fine);
        System.out.println();
        System.out.printf("scale fold: integrated entropy (DOF discarded 9x9 -> 3x3): %.3f bits%n",
                integratedEntropy);
        System.out.println();
        report("COARSE (" + coarseSide + "x" + coarseSide + ")", plexus3, coarseResult);
        System.out.println();

        // ---- verdict (three-way, in exception terms) ----------------------------------------------
        System.out.println(verdict(fine, coarseResult));
    }

    /** The minimal default chain — a single {@link Signature} — so counterterms can surface at each scale. */
    private static List<Invariant> chain(int side) {
        return List.<Invariant>of(new Signature(new Mass(), new Connectivity(), side));
    }

    private static void report(String label, Plexus plexus, CollisionDiagnostic.Result r) {
        System.out.printf("%s: %d contexts, %d collision(s) | blueprint=%s  floor(twins)=%d  "
                        + "separable=%d | total memorised exceptions=%d%n",
                label, plexus.contexts().size(), r.collisions(),
                r.blueprint().isEmpty() ? "{}" : r.blueprint(), r.twinContexts(), r.separableContexts(),
                exceptions(plexus));
    }

    /** Total memorised exceptions across all contexts — read-only via the public context view. */
    private static int exceptions(Plexus plexus) {
        return plexus.contexts().values().stream().mapToInt(Plexus.Context::exceptionCount).sum();
    }

    /** The three-way renormalizability verdict, framed by what the fold did to the exception floor. */
    private static String verdict(CollisionDiagnostic.Result fine, CollisionDiagnostic.Result coarse) {
        final var newCounterterms = new LinkedHashSet<>(coarse.blueprint());
        newCounterterms.removeAll(fine.blueprint());
        final var floorGrew = coarse.twinContexts() > fine.twinContexts();

        if (!floorGrew && newCounterterms.isEmpty())
            return String.format("[RENORMALIZABLE CLOSURE]: the fold preserved the irreducible floor and "
                    + "demanded no new counterterm. The law is stable at a finite fixed-point chain across "
                    + "scales. (floor %d->%d, blueprint %s->%s)",
                    fine.twinContexts(), coarse.twinContexts(), fine.blueprint(), coarse.blueprint());
        if (floorGrew)
            return String.format("[NON-RENORMALIZABLE SHATTERING]: the scale fold manufactured %d new "
                    + "IRREDUCIBLE exception(s) (provable twins). Information destroyed by coarse-graining "
                    + "is unrecoverable by any invariant. (floor %d->%d)",
                    coarse.twinContexts() - fine.twinContexts(), fine.twinContexts(), coarse.twinContexts());
        return String.format("[NON-RENORMALIZABLE (soft / counterterm growth)]: coarse scale demands new "
                + "reducible counterterm(s): %s. Renormalizable only with an enlarged invariant chain. "
                + "(blueprint %s->%s)", newCounterterms, fine.blueprint(), coarse.blueprint());
    }

    // ---- dataset synthesis: D4 orbits placed across the frame ------------------------------------

    /** Place every orientation at every translation that fits the {@code side×side} frame, with {@code label}. */
    private static void placeAll(List<int[]> grids, List<Integer> labels, List<int[][]> patterns,
                                 int side, int label) {
        for (final var cells : patterns) {
            var h = 0;
            var w = 0;
            for (final var p : cells) { h = Math.max(h, p[0] + 1); w = Math.max(w, p[1] + 1); }
            for (var top = 0; top + h <= side; top++)
                for (var left = 0; left + w <= side; left++) {
                    final var g = new int[side * side];
                    for (final var p : cells) g[(top + p[0]) * side + (left + p[1])] = 1;
                    grids.add(g);
                    labels.add(label);
                }
        }
    }

    /** The (up to 8) distinct D4 images of a base cell-set, each normalised to the top-left of its box. */
    private static List<int[][]> orientations(int[][] base) {
        final Set<String> seen = new LinkedHashSet<>();
        final var out = new ArrayList<int[][]>();
        for (final var start : List.of(base, reflect(base)))
            for (var r = 0; r < 4; r++) {
                final var norm = normalize(rotate(start, r));
                if (seen.add(key(norm))) out.add(norm);
            }
        return out;
    }

    private static int[][] rotate(int[][] cells, int times) {
        var c = cells;
        for (var t = 0; t < times; t++) {
            final var n = new int[c.length][2];
            for (var i = 0; i < c.length; i++) { n[i][0] = c[i][1]; n[i][1] = -c[i][0]; }   // (r,c) -> (c,-r)
            c = n;
        }
        return c;
    }

    private static int[][] reflect(int[][] cells) {
        final var n = new int[cells.length][2];
        for (var i = 0; i < cells.length; i++) { n[i][0] = cells[i][0]; n[i][1] = -cells[i][1]; }
        return n;
    }

    private static int[][] normalize(int[][] cells) {
        var minR = Integer.MAX_VALUE;
        var minC = Integer.MAX_VALUE;
        for (final var p : cells) { minR = Math.min(minR, p[0]); minC = Math.min(minC, p[1]); }
        final var n = new int[cells.length][2];
        for (var i = 0; i < cells.length; i++) { n[i][0] = cells[i][0] - minR; n[i][1] = cells[i][1] - minC; }
        return n;
    }

    private static String key(int[][] cells) {
        return Arrays.stream(cells).map(p -> p[0] + "," + p[1]).sorted().collect(Collectors.joining(";"));
    }
}
