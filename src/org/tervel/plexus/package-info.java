/**
 * Plexus — a symmetry-routed exact classifier. It is a <b>router, not a network</b>: an input grid is
 * resolved to a {@link org.tervel.plexus.Plexus.Context Context} by pure structural routing — a symmetry
 * verdict (radial vs the oriented stabilizer) refined by an injected chain of {@code Invariant}s — and the
 * context owns its label (a majority, overridden for the contradictions the chain could not resolve by an
 * injected {@code ResidualResolver}, by default an exact match against them). No weights, thresholds, or statistics. See
 * the {@code docs/} directory for the full design.
 *
 * <h2>Layout</h2>
 * <ul>
 *   <li>{@link org.tervel.plexus.Plexus} — the router plus the sealed {@code Key} (radial/oriented tracks),
 *       {@code Context} (the classifier), the three-state {@code Verdict}, and {@code Sector} /
 *       {@code ChainPolicy} (per-symmetry-class chains).</li>
 *   <li>{@link org.tervel.plexus.Operation} — {@code Plexus → T}, the spine every operation extends.</li>
 *   <li>{@code cli/} — the application: {@code Main}, the interpreter that parses a dataset, trains, and
 *       runs the REPL. It is the only entry point, and it is deliberately outside the library core —
 *       the CLI depends on the router, never the reverse.</li>
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
 *       prime = scale-rigid), {@code Topology} (the context-table walk automaton).</li>
 *   <li>{@code trie/} — {@code Trie}: the routing table stored as self-describing paths ({@code Axis} on
 *       each node, {@code Member} rows at the leaves, {@code Resolution} per walk), so every prefix is
 *       a live context; lazy pure-stop reads, online walk-as-update writes, no seal pass. Two knobs — the
 *       extension vocabulary and the leaf memory — give the fixed-chain configuration and the self-extending
 *       one, where a colliding insert grows an edge locally (the column chosen by the injected
 *       {@code ExtensionPolicy}) and memorises only the provable twins.</li>
 *   <li>{@code diagnostics/} — {@code CollisionDiagnostic} (automated invariant discovery),
 *       {@code ChainCritic} (vet a coordinate before admitting it; extend-or-stop per collision), the
 *       {@code Primitive} battery, {@code Information} (entropy and information gain, shared with the
 *       trie's extension policy), {@code CellGraph}.</li>
 *   <li>{@code reports/} — {@code Report} plus the menu operations (score, topology, possibility, volume,
 *       prime, the slices and queries), and {@code TrieReport} (the store's prefix census, menu
 *       {@code [t]}).</li>
 *   <li>{@code utility/} — {@code Utility} (payoff), {@code Strategy} (the safe↔risky dial, dowry =
 *       neutral), {@code OptimalStop} (the secretary harness).</li>
 * </ul>
 */
package org.tervel.plexus;
