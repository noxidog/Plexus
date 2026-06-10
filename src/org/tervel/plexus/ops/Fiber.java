package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.invariants.Signature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Enumerates the descriptor-fiber {@code D⁻¹(key)} — the inverse of the classifier's routing map, in the
 * generative direction. (This is an <em>enumeration / inversion</em>, not a "generation": exact,
 * exhaustive, deterministic constraint solving over the bounding-box frame, never sampling.)
 *
 * <p>This is the descriptor-direction inverse, the sibling of {@link Searcher}'s symmetry-direction sweep.
 * Like the Searcher it is an {@link Operation}: configured with the {@code key} to invert and the grid
 * {@code side}, applied to a {@link Plexus} it yields the fiber as a {@code Stream<int[]>}.
 */
public final class Fiber implements Operation<Stream<int[]>> {

    private final Plexus.Key key;
    private final int side;

    public Fiber(Plexus.Key key, int side) {
        this.key = key;
        this.side = side;
    }

    /**
     * The descriptor-fiber {@code D⁻¹(key)}: every {@code side×side} grid that routes to <em>this exact
     * node</em> — same symmetry verdict, same discriminator coordinates, position included.
     *
     * <p>It unpacks the mass from the key's leading {@code Signature} coordinate ({@code Signature.massOf}) — no
     * example grid needed, so it works for pure contexts that stored none — then enumerates in the
     * bounding-box frame: for each candidate box shape {@code bh×bw}, the tight {@code bh×bw} patterns of
     * that mass, placed at every position, keeping only candidates whose descriptor equals the key (which
     * enforces the residual symmetry / connectivity / position constraints exactly). The box is not a key
     * column, so it tries every box shape and lets the descriptor filter cut the wrong ones — still
     * bounded (tight patterns of a fixed mass); only the placement count grows with {@code side}.
     */
    @Override
    public Stream<int[]> apply(Plexus plexus) {
        final var coords = key.coords();
        if (coords.isEmpty()) return Stream.empty();
        final var mass = Signature.massOf(coords.get(0), side);    // Signature leads; unpack its mass (V)
        if (mass == 0) {                                          // empty grid is the only mass-0 member
            final var empty = new int[side * side];
            return plexus.descriptor(empty).equals(key) ? Stream.of(empty) : Stream.empty();
        }
        final var seen = new LinkedHashSet<String>();
        final var out = new ArrayList<int[]>();
        for (var bh = 1; bh <= side; bh++)
            for (var bw = 1; bw <= side; bw++) {
                if (bh * bw < mass) continue;                      // box can't hold this mass
                final int h = bh, w = bw;
                combinations(h * w, mass)
                        .map(idx -> grid(h * w, idx))
                        .filter(p -> tight(p, h, w))               // box is exactly h×w (no empty margin)
                        .forEach(p -> {
                            for (var top = 0; top + h <= side; top++)
                                for (var left = 0; left + w <= side; left++) {
                                    final var g = embed(p, h, w, side, top, left);
                                    if (plexus.descriptor(g).equals(key) && seen.add(Arrays.toString(g)))
                                        out.add(g);
                                }
                        });
            }
        return out.stream();
    }

    /** The fiber's size {@code |D⁻¹(key)|} — the combinatorial volume whose log is the entropy (§11). */
    public long count(Plexus plexus) {
        return apply(plexus).count();
    }

    // ---- bounding-box enumeration helpers ----------------------------------------------------

    /** Whether the {@code bh×bw} pattern touches all four borders — i.e. its own box is exactly bh×bw. */
    private static boolean tight(int[] p, int bh, int bw) {
        var top = false; var bottom = false; var leftEdge = false; var rightEdge = false;
        for (var r = 0; r < bh; r++) for (var c = 0; c < bw; c++) if (p[r * bw + c] == 1) {
            if (r == 0) top = true;
            if (r == bh - 1) bottom = true;
            if (c == 0) leftEdge = true;
            if (c == bw - 1) rightEdge = true;
        }
        return top && bottom && leftEdge && rightEdge;
    }

    /** Place a {@code bh×bw} pattern into a {@code side×side} grid with top-left corner at (top,left). */
    private static int[] embed(int[] p, int bh, int bw, int side, int top, int left) {
        final var g = new int[side * side];
        for (var r = 0; r < bh; r++) for (var c = 0; c < bw; c++)
            if (p[r * bw + c] == 1) g[(top + r) * side + (left + c)] = 1;
        return g;
    }

    /** Lazy lexicographic stream of the {@code C(n,k)} strictly-increasing k-subsets of {@code [0,n)}. */
    static Stream<int[]> combinations(int n, int k) {
        if (k < 0 || k > n) return Stream.empty();
        final var it = new Iterator<int[]>() {
            private int[] current = firstCombination(k);   // [0,1,…,k-1], or [] for k=0
            public boolean hasNext() { return current != null; }
            public int[] next() {
                final var ret = current.clone();
                current = nextCombination(current, n);
                return ret;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
    }

    private static int[] firstCombination(int k) {
        final var c = new int[k];
        for (var i = 0; i < k; i++) c[i] = i;
        return c;
    }

    /** The next k-subset in lexicographic order, or {@code null} when {@code c} is the last one. */
    private static int[] nextCombination(int[] c, int n) {
        final var k = c.length;
        var i = k - 1;
        while (i >= 0 && c[i] == n - k + i) i--;   // rightmost index with room to grow
        if (i < 0) return null;                    // c was [n-k, …, n-1] — the last subset
        final var next = c.clone();
        next[i]++;
        for (var j = i + 1; j < k; j++) next[j] = next[j - 1] + 1;
        return next;
    }

    /** A length-{@code n} binary grid with the given cell indices set to 1. */
    private static int[] grid(int n, int[] indices) {
        final var g = new int[n];
        for (final var idx : indices) g[idx] = 1;
        return g;
    }
}
