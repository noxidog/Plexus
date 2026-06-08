package org.tervel.plexus.invariants;

/**
 * Connectivity: the edge count {@code E} — orthogonally-adjacent active pairs. D4-invariant — every rigid
 * transform maps orthogonal adjacencies to orthogonal adjacencies — hence a legal routing coordinate.
 * Blind alone (same structure at any size collides), so it is packed with {@link Mass} into a
 * {@link Signature}.
 */
public final class Connectivity implements Invariant {

    private final int rows;   // explicit frame for a non-square domain; <= 0 means "infer square from length"
    private final int cols;

    /** Connectivity over a square grid (frame inferred per-grid as {@code sqrt(length)}). */
    public Connectivity() { this(-1, -1); }

    /** Connectivity over an explicit {@code rows×cols} (rectangular) domain. */
    public Connectivity(int rows, int cols) { this.rows = rows; this.cols = cols; }

    @Override
    public int applyAsInt(int[] g) {
        final var w = cols > 0 ? cols : Invariant.side(g);     // row stride
        final var h = rows > 0 ? rows : Invariant.side(g);
        var edges = 0;
        for (var r = 0; r < h; r++)
            for (var c = 0; c < w; c++) {
                if (g[r * w + c] == 0) continue;
                if (c + 1 < w && g[r * w + c + 1] == 1) edges++;     // horizontal adjacency
                if (r + 1 < h && g[(r + 1) * w + c] == 1) edges++;   // vertical adjacency
            }
        return edges;
    }

    @Override public boolean applies(boolean radial) { return true; }
    @Override public String name() { return "adj"; }
}
