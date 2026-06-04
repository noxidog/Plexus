package org.tervel.plexus.invariants;

/**
 * Connectivity: the edge count {@code E} — orthogonally-adjacent active pairs. D4-invariant — every rigid
 * transform maps orthogonal adjacencies to orthogonal adjacencies — hence a legal routing coordinate.
 * Blind alone (same structure at any size collides), so it is packed with {@link Mass} into a
 * {@link Signature}.
 */
public final class Connectivity implements Invariant {

    @Override
    public int applyAsInt(int[] g) {
        final var side = Invariant.side(g);
        var edges = 0;
        for (var r = 0; r < side; r++)
            for (var c = 0; c < side; c++) {
                if (g[r * side + c] == 0) continue;
                if (c + 1 < side && g[r * side + c + 1] == 1) edges++;   // horizontal adjacency
                if (r + 1 < side && g[(r + 1) * side + c] == 1) edges++; // vertical adjacency
            }
        return edges;
    }

    @Override public boolean applies(boolean radial) { return true; }
    @Override public String name() { return "adj"; }
}
