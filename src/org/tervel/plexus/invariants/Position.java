package org.tervel.plexus.invariants;

/**
 * Position: the centre-of-mass quadrant (0..8). NOT rotation-invariant, so it refines only radial
 * (round) shapes — which have no orientation, making position well-defined again. This is the axis
 * that dissolves same-shape/opposite-label position contradictions (a top-left dot vs a top-right dot).
 */
public final class Position implements Invariant {
    @Override public int applyAsInt(int[] g) {
        final var side = Invariant.side(g);
        double sr = 0, sc = 0; int m = 0;
        for (var i = 0; i < g.length; i++) if (g[i] == 1) { sr += i / side; sc += i % side; m++; }
        if (m == 0) return 0;
        final double r = sr / m, c = sc / m, mid = (side - 1) / 2.0;
        final var v = r < mid ? 0 : r > mid ? 2 : 1;
        final var h = c < mid ? 0 : c > mid ? 2 : 1;
        return v * 3 + h;
    }
    @Override public boolean applies(boolean radial) { return radial; }
    @Override public String name() { return "quadrant"; }
}
