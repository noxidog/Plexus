/**
 * Plexus — a symmetry-routed exact classifier. It is a <b>router, not a network</b>: an input grid is
 * resolved to a {@link org.tervel.plexus.Plexus.Context Context} by pure structural routing — a symmetry
 * verdict (radial vs the oriented stabilizer) refined by an injected chain of {@code Invariant}s — and the
 * context owns its label (a majority, overridden past the twin floor by an injected {@code ResidualResolver},
 * by default an exact store of the irreducible symmetry-twins). No weights, thresholds, or statistics. See
 * {@code PLEXUS_DESIGN.md} for the full design.
 *
 * <h2>Layout</h2>
 * <ul>
 *   <li>{@link org.tervel.plexus.Plexus} — the router plus the sealed {@code Key} (radial/oriented tracks)
 *       and {@code Context} (the classifier).</li>
 *   <li>{@link org.tervel.plexus.Operation} — {@code Plexus → T}, the spine every operation extends.</li>
 *   <li>{@link org.tervel.plexus.Main} — the interpreter: parse a dataset, train, and run the REPL.</li>
 *   <li>{@link org.tervel.plexus.RenormalizationLoopRunner} — the two-scale closure loop (§11, change of
 *       scale); {@link org.tervel.plexus.SultanRunner} — the optimal-stopping demo (§11, deciding without
 *       the volume).</li>
 *   <li>{@code symmetry/} — the group-theoretic core: {@code Transform}, {@code SymmetryGroup},
 *       {@code Dihedral} (d4 square / d2 rectangle), {@code Crystallographic} (the covering split),
 *       {@code Shape} (the frame-free identity).</li>
 *   <li>{@code invariants/} — the {@code Invariant} chain: {@code Mass} and {@code Connectivity} composed
 *       into one {@code Signature} column, plus the opt-in {@code MaxDegree} (path-vs-fork) and
 *       {@code Position} (radial-only).</li>
 *   <li>{@code residual/} — {@code ResidualResolver} (the boundary seam where routing ends) and
 *       {@code ExactExceptions} (the deterministic default).</li>
 *   <li>{@code ops/} — {@code Searcher} (symmetry sweep), {@code Fiber} (descriptor-fiber enumeration),
 *       {@code ThreadVolume} (thread volume = determinant), {@code Decompose} (factor a shape into atoms),
 *       {@code Scale} (block-decimation / change of scale), {@code PrimeFrame} (frame arithmetic:
 *       prime = scale-rigid), {@code Topology} (the trie-walk automaton).</li>
 *   <li>{@code diagnostics/} — {@code CollisionDiagnostic} (automated invariant discovery), the
 *       {@code Primitive} battery, {@code CellGraph}.</li>
 *   <li>{@code reports/} — {@code Report} plus the menu operations (score, topology, possibility, volume,
 *       prime, the slices and queries).</li>
 *   <li>{@code utility/} — {@code Utility} (payoff), {@code Strategy} (the safe↔risky dial, dowry =
 *       neutral), {@code OptimalStop} (the secretary harness).</li>
 * </ul>
 */
package org.tervel.plexus;
