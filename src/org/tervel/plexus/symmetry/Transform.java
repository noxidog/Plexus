package org.tervel.plexus.symmetry;

import java.util.function.UnaryOperator;

/**
 * One rigid transform of a row-major {@code int[]} grid. A {@code Transform} is a grid&rarr;grid map, hence a
 * {@link UnaryOperator}{@code <int[]>}; the geometry is supplied by the single primitive {@link #src}, with
 * {@link #apply} and {@link #fixes} derived from it.
 *
 * <p>The primitive is in <em>source</em> form: {@code out[r*cols+c] = in[src(r,c,rows,cols)]}. The
 * {@code int[]}-only overloads assume a <b>square</b> grid (side = {@code sqrt(length)}) — the historical
 * default; the {@code (rows, cols)} overloads carry an explicit rectangular frame (a non-square domain, D2;
 * see {@link Crystallographic}). The four <em>transposing</em> moves (rot90/rot270 and the diagonal mirrors)
 * are only valid on a square frame; rectangular groups exclude them.
 */
public interface Transform extends UnaryOperator<int[]> {

    /** Human label, e.g. {@code "rot90"} — used in reports and matches the descriptor bit order. */
    String label();

    /** Whether this transform is a rotation (the rotation subgroup is canonicalized over). */
    boolean isRotation();

    /** Source index feeding destination {@code (r,c)} in a {@code rows×cols} grid: {@code out[r*cols+c] = in[src(...)]}. */
    int src(int r, int c, int rows, int cols);

    /** The image of {@code grid} under this transform on an explicit {@code rows×cols} frame. */
    default int[] apply(int[] grid, int rows, int cols) {
        final var out = new int[grid.length];
        for (var r = 0; r < rows; r++)
            for (var c = 0; c < cols; c++)
                out[r * cols + c] = grid[src(r, c, rows, cols)];
        return out;
    }

    /** The image of {@code grid} under this transform, assuming a square frame. */
    @Override
    default int[] apply(int[] grid) {
        final var s = side(grid);
        return apply(grid, s, s);
    }

    /** Whether this transform leaves a {@code rows×cols} {@code grid} unchanged (i.e. it is a symmetry of it). */
    default boolean fixes(int[] grid, int rows, int cols) {
        for (var r = 0; r < rows; r++)
            for (var c = 0; c < cols; c++)
                if (grid[r * cols + c] != grid[src(r, c, rows, cols)]) return false;
        return true;
    }

    /** Whether this transform leaves {@code grid} unchanged, assuming a square frame. */
    default boolean fixes(int[] grid) {
        final var s = side(grid);
        return fixes(grid, s, s);
    }

    /** Side length of a square grid given its cell count. */
    static int side(int[] grid) {
        return (int) Math.round(Math.sqrt(grid.length));
    }
}
