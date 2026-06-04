package org.tervel.plexus.diagnostics;

/**
 * The <b>cell-graph</b> of a binary grid: one node per active cell, one edge per orthogonal adjacency.
 * This is the abstract graph the design reads the {@code Signature} off of (mass {@code = V}, connectivity
 * {@code = E}); the diagnostic computes its richer topological primitives (max degree, Betti numbers,
 * adjacency spectrum) here. Every quantity exposed is a <b>graph isomorphism invariant</b>, so it is
 * automatically invariant under the rigid D4 transforms (which permute cells while preserving adjacency) —
 * the precondition for any of them to be a legal routing coordinate.
 */
final class CellGraph {

    private final int order;          // |V| — active-cell count
    private final int size;           // |E| — orthogonal adjacencies
    private final int[][] adj;        // adjacency lists over the compacted node indices [0, order)

    CellGraph(int[] grid) {
        final var side = (int) Math.round(Math.sqrt(grid.length));
        final var nodeOf = new int[grid.length];
        java.util.Arrays.fill(nodeOf, -1);
        var n = 0;
        for (var i = 0; i < grid.length; i++) if (grid[i] == 1) nodeOf[i] = n++;
        this.order = n;

        final var lists = new java.util.ArrayList<java.util.List<Integer>>();
        for (var v = 0; v < n; v++) lists.add(new java.util.ArrayList<>());
        var e = 0;
        for (var r = 0; r < side; r++)
            for (var c = 0; c < side; c++) {
                final var i = r * side + c;
                if (grid[i] == 0) continue;
                if (c + 1 < side && grid[i + 1] == 1)    { link(lists, nodeOf[i], nodeOf[i + 1]); e++; }
                if (r + 1 < side && grid[i + side] == 1) { link(lists, nodeOf[i], nodeOf[i + side]); e++; }
            }
        this.size = e;
        this.adj = new int[n][];
        for (var v = 0; v < n; v++) adj[v] = lists.get(v).stream().mapToInt(Integer::intValue).toArray();
    }

    private static void link(java.util.List<java.util.List<Integer>> lists, int a, int b) {
        lists.get(a).add(b);
        lists.get(b).add(a);
    }

    int order() { return order; }
    int size()  { return size; }

    /** Maximum vertex degree {@code Δ(G) = max_v deg(v)} (0 on the empty graph). */
    int maxDegree() {
        var max = 0;
        for (final var nbrs : adj) max = Math.max(max, nbrs.length);
        return max;
    }

    /** Connected-component count {@code C} — the zeroth Betti number {@code b₀}. */
    int components() {
        final var seen = new boolean[order];
        var comps = 0;
        final var stack = new java.util.ArrayDeque<Integer>();
        for (var s = 0; s < order; s++) {
            if (seen[s]) continue;
            comps++;
            seen[s] = true;
            stack.push(s);
            while (!stack.isEmpty()) {
                final var v = stack.pop();
                for (final var u : adj[v]) if (!seen[u]) { seen[u] = true; stack.push(u); }
            }
        }
        return comps;
    }

    /** First Betti number {@code b₁ = E − V + C} — independent cycles (enclosed holes). */
    int cycleRank() {
        return size - order + components();
    }

    /**
     * Spectral radius {@code λ₁(A)} — the largest eigenvalue of the adjacency matrix. Computed by power
     * iteration on the positive-shifted matrix {@code B = A + ΔI} (shift {@code Δ = maxDegree} guarantees
     * {@code B ⪰ 0}, so the iteration cannot oscillate on a bipartite ±λ spectrum), then unshifted:
     * {@code λ₁ = μ₁(B) − Δ}. Returns 0 on the empty graph.
     */
    double spectralRadius() {
        if (order == 0) return 0.0;
        final var shift = maxDegree();
        var x = new double[order];
        java.util.Arrays.fill(x, 1.0 / Math.sqrt(order));
        var mu = 0.0;
        for (var iter = 0; iter < 1000; iter++) {
            final var y = new double[order];
            for (var v = 0; v < order; v++) {
                var acc = shift * x[v];                 // (A + ΔI) x
                for (final var u : adj[v]) acc += x[u];
                y[v] = acc;
            }
            var norm = 0.0;
            for (final var yi : y) norm += yi * yi;
            norm = Math.sqrt(norm);
            if (norm == 0.0) break;
            for (var v = 0; v < order; v++) y[v] /= norm;
            final var prev = mu;
            mu = norm;                                   // dominant eigenvalue of B
            x = y;
            if (Math.abs(mu - prev) < 1e-12) break;
        }
        return mu - shift;
    }
}
