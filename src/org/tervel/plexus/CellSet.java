package org.tervel.plexus;

import java.util.Arrays;
import java.util.function.IntBinaryOperator;

/**
 * A shape as a set of cells, translation-free: coordinates {@code (a, b)} shifted so both minima are 0, sorted,
 * packed one cell per {@code int}. On the square lattice {@code (a, b)} is {@code (row, column)}; on the hex
 * lattice it is the axial {@code (q, r)}. Two shapes are equal exactly when one is a translate of the other.
 *
 * <p>The natural order (cell count, then extents, then the packed cells) is the fixed, arbitrary order that
 * "smallest" means wherever a canonical choice is made.
 */
final class CellSet implements Comparable<CellSet> {

    /** A cell map {@code (a, b) -> (a', b')}, applied about the origin; translation is normalized away after. */
    record Map(String name, IntBinaryOperator fa, IntBinaryOperator fb) { }

    private final int[] cells;     // packed (a << 16) | b, sorted ascending
    final int extentA;             // max a + 1
    final int extentB;             // max b + 1

    private CellSet(int[] cells, int extentA, int extentB) {
        this.cells = cells; this.extentA = extentA; this.extentB = extentB;
    }

    /** The cells {@code (as[i], bs[i])}, normalized. Empty input gives the empty set. */
    static CellSet of(int[] as, int[] bs) {
        if (as.length == 0) return new CellSet(new int[0], 0, 0);
        int minA = Integer.MAX_VALUE, minB = Integer.MAX_VALUE, maxA = Integer.MIN_VALUE, maxB = Integer.MIN_VALUE;
        for (var i = 0; i < as.length; i++) {
            minA = Math.min(minA, as[i]); maxA = Math.max(maxA, as[i]);
            minB = Math.min(minB, bs[i]); maxB = Math.max(maxB, bs[i]);
        }
        final var packed = new int[as.length];
        for (var i = 0; i < as.length; i++) packed[i] = ((as[i] - minA) << 16) | (bs[i] - minB);
        Arrays.sort(packed);
        return new CellSet(packed, maxA - minA + 1, maxB - minB + 1);
    }

    int size() { return cells.length; }
    int a(int i) { return cells[i] >>> 16; }
    int b(int i) { return cells[i] & 0xFFFF; }

    boolean contains(int a, int b) {
        return a >= 0 && b >= 0 && a < extentA && b < extentB && Arrays.binarySearch(cells, (a << 16) | b) >= 0;
    }

    /** The image under {@code m}, normalized. */
    CellSet map(Map m) {
        final var as = new int[cells.length];
        final var bs = new int[cells.length];
        for (var i = 0; i < cells.length; i++) {
            as[i] = m.fa().applyAsInt(a(i), b(i));
            bs[i] = m.fb().applyAsInt(a(i), b(i));
        }
        return of(as, bs);
    }

    /** Edges between cells that differ by one of the forward offsets {@code {da, db}} (each edge counted once). */
    int edges(int[][] forward) {
        var e = 0;
        for (var i = 0; i < cells.length; i++)
            for (final var d : forward) if (contains(a(i) + d[0], b(i) + d[1])) e++;
        return e;
    }

    @Override public int compareTo(CellSet o) {
        if (cells.length != o.cells.length) return Integer.compare(cells.length, o.cells.length);
        if (extentA != o.extentA) return Integer.compare(extentA, o.extentA);
        if (extentB != o.extentB) return Integer.compare(extentB, o.extentB);
        return Arrays.compare(cells, o.cells);
    }

    @Override public boolean equals(Object o) { return o instanceof CellSet c && Arrays.equals(cells, c.cells); }
    @Override public int hashCode() { return Arrays.hashCode(cells); }
}
