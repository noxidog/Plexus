package org.tervel.plexus.invariants;

/**
 * Maximum vertex degree {@code Δ(G) = max_v deg(v)} of the cell-graph (nodes = filled cells, edges =
 * orthogonal adjacencies). The <b>branch / path coordinate</b>: it is what makes the {@code V − E} "is it a
 * path" reading (§3) strictly rigorous. {@code E = V − 1} alone only says <em>tree</em> — a connected,
 * acyclic spanning subgraph — which a branched shape satisfies too: the T-tetromino has {@code V=4, E=3}
 * (a tree) yet its centre cell has degree 3, so it is a tree <em>with a fork</em>, not a simple path. A
 * simple path is exactly a tree with {@code Δ ≤ 2}; any {@code Δ ≥ 3} is a branch. So {@code Δ} is the axis
 * that separates a straight line from a fork of equal {@code (V, E)}.
 *
 * <p>D4-invariant — every rigid transform is a graph automorphism of the cell-graph (it permutes cells and
 * preserves orthogonal adjacency), so it preserves every vertex degree and hence their maximum — which is
 * why it is a legal routing coordinate (the invariance contract, {@link Invariant#invariantUnder}).
 *
 * <p>This is the concrete realisation of the {@code max-degree} blueprint the collision diagnostic
 * (`org.tervel.plexus.diagnostics`) emits when it finds a path-vs-fork collision: discovery names the axis,
 * this class is the axis. It is held <b>out of the default chain</b> on purpose (minimality, §3) — no
 * reference dataset has a collision that earns it — but it is ready to drop in for any dataset that does,
 * and once in the chain the {@code Fiber} enumerator filters on it automatically, so a degree-3 fork can no
 * longer slip into a path context's fiber.
 */
public final class MaxDegree implements Invariant {

    @Override
    public int applyAsInt(int[] g) {
        final var side = Invariant.side(g);
        var max = 0;
        for (var r = 0; r < side; r++)
            for (var c = 0; c < side; c++) {
                if (g[r * side + c] == 0) continue;
                var deg = 0;
                if (c > 0        && g[r * side + c - 1] == 1) deg++;
                if (c + 1 < side && g[r * side + c + 1] == 1) deg++;
                if (r > 0        && g[(r - 1) * side + c] == 1) deg++;
                if (r + 1 < side && g[(r + 1) * side + c] == 1) deg++;
                max = Math.max(max, deg);
            }
        return max;
    }

    @Override public boolean applies(boolean radial) { return true; }
    @Override public String name() { return "deg"; }
}
