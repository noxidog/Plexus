package org.tervel.plexus.symmetry;

import java.util.function.UnaryOperator;

/**
 * One rigid transform of a square grid, represented as a flat row-major {@code int[]} (side =
 * {@code sqrt(length)}). A {@code Transform} is a grid&rarr;grid map, hence a
 * {@link UnaryOperator}{@code <int[]>}; the geometry is supplied by the single primitive
 * {@link #src}, with {@link #apply} and {@link #fixes} derived from it.
 *
 * <p>The primitive is in <em>source</em> form: {@code out[r*side+c] = in[src(r,c,side)]}.
 */
public interface Transform extends UnaryOperator<int[]> {

    /** Human label, e.g. {@code "rot90"} — used in reports and matches the descriptor bit order. */
    String label();

    /** Whether this transform is a rotation (the rotation subgroup is canonicalized over). */
    boolean isRotation();

    /** Source index feeding destination {@code (r,c)}: {@code out[r*side+c] = in[src(r,c,side)]}. */
    int src(int r, int c, int side);

    /** The image of {@code grid} under this transform. */
    @Override
    default int[] apply(int[] grid) {
        final var side = side(grid);
        final var out = new int[grid.length];
        for (var r = 0; r < side; r++)
            for (var c = 0; c < side; c++)
                out[r * side + c] = grid[src(r, c, side)];
        return out;
    }

    /** Whether this transform leaves {@code grid} unchanged (i.e. it is a symmetry of {@code grid}). */
    default boolean fixes(int[] grid) {
        final var side = side(grid);
        for (var r = 0; r < side; r++)
            for (var c = 0; c < side; c++)
                if (grid[r * side + c] != grid[src(r, c, side)]) return false;
        return true;
    }

    /** Side length of a square grid given its cell count. */
    static int side(int[] grid) {
        return (int) Math.round(Math.sqrt(grid.length));
    }
}
