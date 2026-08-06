package org.tervel.plexus.diagnostics;

import java.util.Collection;
import java.util.List;

/**
 * Shannon information over a binary label, and the one question every consumer actually asks of it:
 * <b>how much does partitioning by some coordinate reduce the uncertainty?</b>
 *
 * <p>Two places compute exactly this: {@link CollisionDiagnostic}, scoring the
 * {@link Primitive} battery against a collision to name the next invariant, and the trie's extension policy,
 * scoring the same battery against a colliding leaf to choose an edge to grow. They differ only in what they
 * hold (labelled grids vs. stored members); the arithmetic is identical, so it lives here once.
 *
 * <p>It sits in {@code diagnostics} because that is where information-gain already belonged, and because the
 * dependency runs the right way: {@code trie} already knows about {@code diagnostics} (it splits on
 * {@link Primitive}), while {@code diagnostics} knows nothing about the trie.
 */
public final class Information {

    /** Binary Shannon entropy in bits; {@code 0} for an empty or one-sided count. */
    public static double entropy(long negative, long positive) {
        final var n = negative + positive;
        if (n == 0) return 0.0;
        final var p = (double) positive / n;
        if (p == 0.0 || p == 1.0) return 0.0;
        return -(p * log2(p) + (1 - p) * log2(1 - p));
    }

    /** {@link #entropy(long, long)} over a {@code [neg, pos]} pair. */
    public static double entropy(long[] counts) { return entropy(counts[0], counts[1]); }

    /**
     * The <b>information gain</b> of a partition: the whole's entropy less the size-weighted mean entropy of
     * its parts. Zero when every part is as mixed as the whole (the coordinate separates nothing); equal to
     * the base entropy when every part is pure (an exact split).
     */
    public static double gain(long[] whole, Collection<long[]> parts) {
        final var total = whole[0] + whole[1];
        if (total == 0) return 0.0;
        var residual = 0.0;
        for (final var part : parts) residual += (double) (part[0] + part[1]) / total * entropy(part);
        return entropy(whole) - residual;
    }

    /** Tally a list of 0/1 labels into a {@code [neg, pos]} pair. */
    public static long[] counts(List<Integer> labels) {
        final var out = new long[2];
        for (final var l : labels) out[l == 1 ? 1 : 0]++;
        return out;
    }

    static double log2(double x) { return Math.log(x) / Math.log(2); }

    private Information() { }
}
