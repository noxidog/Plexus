package org.tervel.plexus;

import java.util.Arrays;

/**
 * Several planes of one frame carried into a reference shape's canonical frame by one motion
 * ({@link Descriptor#toCanonical}). Coordinates are those of the reference's canonical pose: on the square
 * lattice {@code (row, column)}, on hex the axial {@code (q, r)}, with the reference's canonical shape
 * starting at {@code (0, 0)}. Cells of the other planes keep their place relative to the reference, so they
 * can lie outside its box and have negative coordinates.
 *
 * <p>Two folds are equal when every plane holds the same cells. The natural order (plane by plane: cell
 * count, then the cells in order) is the order "smallest" means when a symmetric reference leaves several
 * motions to choose from.
 */
public final class Fold implements Comparable<Fold> {

    private final int[][] planes;   // per plane: a0, b0, a1, b1, ... sorted by (a, b)

    private Fold(int[][] planes) { this.planes = planes; }

    /** A fold of {@code as[p][i], bs[p][i]}, each plane's cells sorted. */
    static Fold of(int[][] as, int[][] bs) {
        final var out = new int[as.length][];
        for (var p = 0; p < as.length; p++) {
            final var n = as[p].length;
            final var packed = new long[n];
            for (var i = 0; i < n; i++) packed[i] = ((long) as[p][i] << 32) ^ ((bs[p][i] & 0xFFFFFFFFL) ^ 0x80000000L);
            Arrays.sort(packed);
            final var flat = new int[2 * n];
            for (var i = 0; i < n; i++) {
                flat[2 * i] = (int) (packed[i] >> 32);
                flat[2 * i + 1] = (int) ((packed[i] & 0xFFFFFFFFL) ^ 0x80000000L);
            }
            out[p] = flat;
        }
        return new Fold(out);
    }

    /** How many planes were folded. */
    public int planes() { return planes.length; }

    /** Plane {@code p}'s cells as {@code {a, b}} pairs, in order. */
    public int[][] cells(int p) {
        final var flat = planes[p];
        final var out = new int[flat.length / 2][];
        for (var i = 0; i < out.length; i++) out[i] = new int[]{flat[2 * i], flat[2 * i + 1]};
        return out;
    }

    @Override public int compareTo(Fold o) {
        for (var p = 0; p < Math.min(planes.length, o.planes.length); p++) {
            if (planes[p].length != o.planes[p].length) return Integer.compare(planes[p].length, o.planes[p].length);
            final var c = Arrays.compare(planes[p], o.planes[p]);
            if (c != 0) return c;
        }
        return Integer.compare(planes.length, o.planes.length);
    }

    @Override public boolean equals(Object o) { return o instanceof Fold f && Arrays.deepEquals(planes, f.planes); }
    @Override public int hashCode() { return Arrays.deepHashCode(planes); }

    @Override public String toString() {
        final var sb = new StringBuilder("fold");
        for (var p = 0; p < planes.length; p++) {
            sb.append(p == 0 ? " " : " | ");
            for (var i = 0; i < planes[p].length; i += 2)
                sb.append(i == 0 ? "" : " ").append('(').append(planes[p][i]).append(',').append(planes[p][i + 1]).append(')');
        }
        return sb.toString();
    }
}
