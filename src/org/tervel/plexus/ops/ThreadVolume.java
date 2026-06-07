package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;

import java.util.Arrays;
import java.util.function.ToIntFunction;

/**
 * <b>The thread is the determinant</b>, made executable. §11's "least action threads the intersection" and
 * "entropy = log|fiber|" name the descriptor-fiber {@code D⁻¹(key)} — the residual channel a grid is pinned
 * to once the conserved coordinates are fixed — the <em>thread</em>. This operation measures that thread's
 * volume <b>five ways</b> at one node, turning the slogan into numbers that can be compared:
 *
 * <ul>
 *   <li><b>{@code |fiber|}</b> — the combinatorial volume: how many {@code side×side} grids route here
 *       ({@link Fiber}{@code .count}). The thing §11 calls {@code |fiber|}, whose log is the entropy.</li>
 *   <li><b>{@code |orbit|}</b> — the <b>rigid orbit</b>: the distinct images of the centred representative
 *       under the group ({@code |G| / |Stab|} by orbit–stabilizer). This is the fiber in the bounding-box
 *       frame — translation quotiented out — and it is where the exact identity lives.</li>
 *   <li><b>{@code [G:Stab]}</b> — the <b>orbit-stabilizer index</b> {@code |G| / |Stab|} computed from the
 *       stabilizer directly: the group-theoretic determinant of the action. {@code Shape.isotropy =
 *       |Stab|/|G|} is its reciprocal. The exact, integer reading (oriented track only — the radial track
 *       carries no stabilizer column).</li>
 *   <li><b>{@code √det(J Jᵀ)}</b> — the <b>Gram determinant of the descriptor's Jacobian</b>: stack the
 *       finite-difference gradients of the invariant <em>components</em> (unpacked, never the packed
 *       {@code Signature} value — see {@link org.tervel.plexus.invariants.Invariant#components()}) into a
 *       matrix {@code J}, and {@code √det(J Jᵀ)} is the discrete co-volume the coordinates carve out. The
 *       coarea/change-of-variables reading — the one that <em>literally</em> means "the thread is the
 *       determinant."</li>
 *   <li><b>{@code det L}</b> — the <b>Matrix-Tree determinant</b>: the number of spanning trees of the cell
 *       graph, {@code det} of its reduced Laplacian. The graph-theoretic volume, tied to the §3 {@code V−E}
 *       path/tree/loop dial.</li>
 * </ul>
 *
 * <p>The exact identity it checks is the <b>orbit-stabilizer theorem</b>, {@code |orbit| == [G:Stab]}: the
 * thread, measured in the bounding-box frame, <em>is</em> the group determinant. The full-frame {@code |fiber|}
 * sits alongside and is in general larger — by exactly the <b>translational multiplicity</b> (how many places
 * the box fits in the {@code side×side} grid), the residual entropy the rigid group can't see. So the report
 * shows both: the identity closes where it is exactly true (the orbit), and the translation gap stays visible
 * as {@code |fiber| / |orbit|} rather than being smoothed over.
 */
public final class ThreadVolume implements Operation<ThreadVolume.Measure> {

    /** The thread's volume by all readings; {@code index < 0} marks the radial track (no stabilizer). */
    public record Measure(long fiber, long orbit, long index, double gram, long spanningTrees, boolean radial) {
        /** The exact reading: the orbit-stabilizer theorem — the rigid orbit equals the group index. */
        public boolean identityHolds() { return !radial && index > 0 && orbit == index; }
    }

    private final Plexus.Key key;
    private final int side;

    public ThreadVolume(Plexus.Key key, int side) {
        this.key = key;
        this.side = side;
    }

    @Override
    public Measure apply(Plexus plexus) {
        final var members = new Fiber(key, side).apply(plexus).toList();
        final long fiber = members.size();
        // [G:Stab] = |G| / |Stab|; the canonical stabilizer always contains identity, so popcount ≥ 1.
        final long index = key.radial() ? -1
                : (long) plexus.group().transforms().size() / Integer.bitCount(key.symmetry());
        // The Gram and Matrix-Tree volumes are point quantities; evaluate them at a representative member.
        final int[] rep = members.isEmpty() ? null : members.get(0);
        final long orbit = rep == null ? 0 : orbit(plexus, rep);
        final double gram = rep == null ? 0.0 : gramVolume(plexus, rep);
        final long trees = rep == null ? 0 : spanningTrees(rep);
        return new Measure(fiber, orbit, index, gram, trees, key.radial());
    }

    /** The rigid orbit: the distinct images of the centred representative under the group (the bbox-frame fiber). */
    private long orbit(Plexus plexus, int[] rep) {
        final var centered = Plexus.center(rep);
        if (centered == null) return 0;
        final var images = new java.util.HashSet<String>();
        for (final var t : plexus.group().transforms()) images.add(Arrays.toString(t.apply(centered)));
        return images.size();
    }

    // ---- the Jacobian / Gram volume --------------------------------------------------------------

    /** {@code √det(J Jᵀ)} — the co-volume the invariant-component gradients span at {@code g}. */
    private double gramVolume(Plexus plexus, int[] g) {
        final var rows = plexus.invariants().stream()
                .filter(inv -> inv.applies(key.radial()))
                .flatMap(inv -> inv.components().stream())   // unpack composites (Signature → V, E)
                .map(comp -> gradient(comp, g))
                .toList();
        final var n = rows.size();
        final var gram = new double[n][n];
        for (var i = 0; i < n; i++)
            for (var j = 0; j < n; j++)
                gram[i][j] = dot(rows.get(i), rows.get(j));
        return Math.sqrt(Math.max(0.0, det(gram)));   // det(J Jᵀ) ≥ 0; clamp away float noise
    }

    /** Discrete gradient of a scalar coordinate: how it moves when each cell flips (single-cell difference). */
    static double[] gradient(ToIntFunction<int[]> f, int[] g) {
        final var base = f.applyAsInt(g);
        final var d = new double[g.length];
        for (var c = 0; c < g.length; c++) {
            final var flipped = g.clone();
            flipped[c] ^= 1;
            d[c] = f.applyAsInt(flipped) - base;
        }
        return d;
    }

    private static double dot(double[] a, double[] b) {
        var s = 0.0;
        for (var i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    // ---- the Matrix-Tree volume ------------------------------------------------------------------

    /**
     * The number of spanning trees of the cell graph — {@code det} of its reduced Laplacian (Matrix-Tree).
     * One isolated cell has the trivial spanning tree (1); a disconnected graph has none (0).
     */
    private long spanningTrees(int[] g) {
        final var idx = new int[g.length];
        Arrays.fill(idx, -1);
        var n = 0;
        for (var i = 0; i < g.length; i++) if (g[i] == 1) idx[i] = n++;
        if (n <= 1) return n;                            // empty → 0, single cell → 1 (trivial tree)

        final var lap = new double[n][n];
        for (var r = 0; r < side; r++)
            for (var c = 0; c < side; c++) {
                final var i = r * side + c;
                if (g[i] != 1) continue;
                if (c + 1 < side && g[i + 1] == 1)    edge(lap, idx[i], idx[i + 1]);
                if (r + 1 < side && g[i + side] == 1) edge(lap, idx[i], idx[i + side]);
            }
        // Delete the last row/column; the determinant of any cofactor is the spanning-tree count.
        final var m = n - 1;
        final var minor = new double[m][m];
        for (var i = 0; i < m; i++)
            System.arraycopy(lap[i], 0, minor[i], 0, m);
        return Math.round(det(minor));
    }

    /** Add one Laplacian edge {@code (a,b)}: {@code +1} on both degrees, {@code −1} on the off-diagonals. */
    private static void edge(double[][] lap, int a, int b) {
        lap[a][a]++; lap[b][b]++;
        lap[a][b]--; lap[b][a]--;
    }

    // ---- shared dense determinant ----------------------------------------------------------------

    /** Determinant by Gaussian elimination with partial pivoting; 0 if singular. */
    static double det(double[][] a) {
        final var n = a.length;
        if (n == 0) return 1.0;
        final var m = new double[n][];
        for (var i = 0; i < n; i++) m[i] = a[i].clone();
        var det = 1.0;
        for (var col = 0; col < n; col++) {
            var pivot = col;
            for (var r = col + 1; r < n; r++) if (Math.abs(m[r][col]) > Math.abs(m[pivot][col])) pivot = r;
            if (Math.abs(m[pivot][col]) < 1e-9) return 0.0;
            if (pivot != col) { final var t = m[pivot]; m[pivot] = m[col]; m[col] = t; det = -det; }
            det *= m[col][col];
            for (var r = col + 1; r < n; r++) {
                final var f = m[r][col] / m[col][col];
                for (var k = col; k < n; k++) m[r][k] -= f * m[col][k];
            }
        }
        return det;
    }
}
