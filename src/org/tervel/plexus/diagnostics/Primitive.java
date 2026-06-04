package org.tervel.plexus.diagnostics;

import java.util.List;

/**
 * A <b>universal topological primitive</b> — a candidate group-invariant coordinate the diagnostic tests
 * against a collision. Each is a graph-isomorphism invariant of the {@link CellGraph} (hence D4-invariant,
 * the contract a routing coordinate must honour), carries a human {@link #name()}, and emits its
 * {@link #spec() formal mathematical specification} — the exact blueprint a developer would hand-implement
 * as the next {@code Invariant} extension.
 *
 * <p>The value is a {@code double} (the spectrum is real); collisions are partitioned by its
 * {@link #quantized} value so floating-point primitives bucket cleanly alongside the integer ones.
 */
public interface Primitive {

    String name();

    /** The formal mathematical definition — the blueprint for the manual invariant. */
    String spec();

    /** The primitive's value on a grid's cell-graph. */
    double valueOf(int[] grid);

    /** The value rounded to a stable bucket key (1e-6), so real spectra partition like the integer ones. */
    default long quantized(int[] grid) { return Math.round(valueOf(grid) * 1_000_000.0); }

    /** The standard battery of primitives the diagnostic sweeps, in escalating structural richness. */
    static List<Primitive> battery() {
        return List.of(
                of("max-degree",
                   "Δ(G) = max_{v∈V} deg(v),  deg(v) = |{u∈V : u ~ v}| (orthogonal adjacency). "
                   + "Separates branched cores (Δ≥3, e.g. the T's fork) from paths/cycles (Δ≤2).",
                   g -> new CellGraph(g).maxDegree()),
                of("components-b0",
                   "b₀(G) = C = number of connected components (0th Betti number). "
                   + "Separates a single connected blob from a fragmented one of equal (V,E).",
                   g -> new CellGraph(g).components()),
                of("cycle-rank-b1",
                   "b₁(G) = E − V + C (1st Betti number) = independent cycles / enclosed holes. "
                   + "Separates a tree/path (b₁=0) from a loop or thick region (b₁≥1).",
                   g -> new CellGraph(g).cycleRank()),
                of("spectral-radius",
                   "λ₁(A) = max eigenvalue of the adjacency matrix A (A_{uv}=1 iff u~v). "
                   + "A permutation-invariant of the cell-graph; refines degree/branching structure.",
                   g -> new CellGraph(g).spectralRadius()));
    }

    private static Primitive of(String name, String spec, java.util.function.ToDoubleFunction<int[]> f) {
        return new Primitive() {
            @Override public String name()            { return name; }
            @Override public String spec()            { return spec; }
            @Override public double valueOf(int[] g)  { return f.applyAsDouble(g); }
        };
    }
}
