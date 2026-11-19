package org.tervel.plexus;

import java.util.Collection;
import java.util.List;

/**
 * Shannon information over a binary label, and the one question self-extension asks of it: how much does
 * splitting a mixed node by some coordinate reduce the uncertainty? {@link Plexus} uses it to pick, from the
 * {@link Primitive} battery, the shape coordinate to insert where different shapes collide.
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
