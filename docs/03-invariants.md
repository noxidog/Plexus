# The Invariant Chain

*The orthogonal refinements layered on the symmetry verdict, and the contract they owe.*

---

## 3. The invariant chain (orthogonal refinements)

The symmetry verdict quotients a shape into its orbit. **Invariants** refine that orbit with orthogonal coordinates. They are an **injected chain** — the caller composes "do symmetry, plus refine by these."

Each `Invariant` *is* a coordinate map `grid → int` (`extends ToIntFunction<int[]>`, so the coordinate is `applyAsInt(grid)`), declares `boolean applies(radial)`, a `name()`, and carries its contract as an executable default method `invariantUnder(Transform, grid)` (`applyAsInt(t.apply(g)) == applyAsInt(g)`). The current chain:

| Invariant | Coordinate | Applies to |
|---|---|---|
| **Signature** | mass `V` and connectivity `E` packed as `V·W + E` | all shapes |
| **MaxDegree** *(opt-in)* | max vertex degree `Δ` (path vs fork) | all shapes |
| **Position** *(opt-in)* | centre-of-mass quadrant | radial only |

`Signature` is the **two invariant graph quantities bundled into one column** — composed from a separate `Mass` invariant (`V`, cell count) and `Connectivity` invariant (`E`, edge count), wired together in `cli.Main` as `new Signature(new Mass(), connectivity, n)` — the `Connectivity` carries the frame on a rectangular domain, and the third argument is not itself the base: the base is `Signature.width(n) = 2·n·(n−1) + 1`, derived from it. `Main` passes `n = rows` on a square (so `width` is exactly `maxEdges + 1`) and `n = rows + cols` on a rectangle, where `width(rows + cols)` comfortably exceeds the rectangle's `2·rows·cols − rows − cols` edge ceiling — the argument is a frame handle, the base is what `width` makes of it. They are one invariant on purpose — *separately each is blind in one direction* (mass alone admits a shattered blob of equal count; connectivity alone admits a same-structure shape of different size), and only their conjunction pins size **and** structure. The packing `V·W + E` (with `W = maxEdges+1`) is injective, so "same signature" iff "same mass *and* same connectivity"; `Fiber` unpacks the mass back via `Signature.massOf`. Both `V` and `E` are D4-invariant (every rigid transform preserves cell count and adjacency count), so the bundle honours the invariance contract. `Position` is **opt-in** (radial-only); it earns its place only on dot-position contradictions.

---

### The invariance contract

The load-bearing rule: **an invariant must be invariant under the routing symmetry**, or it undoes the canonicalization and shatters the orbit. The rule is executable, not just prose — `Invariant.invariantUnder(t, g)` checks it per transform. Mass and connectivity are rotation-invariant ✓. **Position is not** — and that is exactly why it `applies` only to **radial** shapes, which have no orientation, so position becomes well-defined again. (Position is the axis that dissolves same-shape/opposite-label *position* contradictions, e.g. a top-left dot vs a top-right dot.)

Each axis is earned by a specific pair of shapes that collide on all prior axes: symmetry isolates the round shapes; connectivity separates a connected shape from a split blob of equal mass; mass separates a shape from a heavier look-alike; position separates two otherwise-identical placements. The chain is the extension point — add an axis only where a collision demands it, and only invariant under the axes already present.

---

### Reading the columns as cell-graph topology

The `Signature`'s two packed components are not arbitrary — together they describe the shape as a **graph**: each filled cell is a node, each orthogonal contact an edge. Then:

- **mass = V** — the number of nodes (cells).
- **connectivity = E** — the number of edges (orthogonal links).
- **span = the bounding box** — how far the graph reaches (a *structural* property, not a routing column): the bounding box touches all four of its own borders, so the shape crosses it in both directions. When the box equals the full grid it reaches every edge of the *ambient* box — a candidate for a path all the way across.

The combination that reads as **"is it a path"** is `V − E`:

| relation | topology |
|---|---|
| `E = V − 1` | one connected component, no loop — a **tree / path** across the box |
| `E < V − 1` | **fragments** — disconnected pieces, no path between them |
| `E > V − 1` | a **cycle / loop** or a thick region — more than a path |

So a *genuine path across the full box* is `span = full grid` **and** `E = V − 1` (spans, connected, one route). The **span** says *does it reach across*, `V` says *how long*, `E` says *how connected* — and `V − E` is the dial between fragments, path, and loop. `V` and `E` are the routing info (packed into the `Signature` column); span is structural.

The honest caveat: with only `V` and `E` in the key this is the **unoriented** graph — no start, end, or direction, and no degree — so `E = V − 1` only certifies *tree* (connected, acyclic, spanning), not *simple path*. The T is the giveaway: a tree of `V=4, E=3`, but its centre cell has **degree 3**, so it is a spanning *tree with a fork*, not a path. The rigorous discriminator is **maximum degree** `Δ`: a tree is a simple path iff `Δ ≤ 2`, and any `Δ ≥ 3` is a branch. That coordinate exists as the opt-in **`MaxDegree`** invariant (`Δ(G) = max_v deg(v)`, a graph-automorphism invariant and so D4-legal) — the implemented form of the very axis the collision diagnostic blueprints on a path-vs-fork collision ([§3](08-diagnostics.md), *automated invariant discovery*). It is held out of the default chain by minimality (the T-orbit reference datasets do not earn it; `separable-path-fork` is the dataset built to, where the diagnostic emits its blueprint), but **once added it closes the gap in both directions**: routing then separates a line from a fork of equal `(V, E)`, and because `Fiber` filters candidates by the full descriptor, a degree-3 fork can no longer appear as a **false positive** in a path context's fiber.

---

### Minimality

The chain is **minimal**: one `Signature` drives the exact-memory exceptions down to the irreducible floor — the genuine twins — and richer invariants (components, max-degree) resolve no *further* collision **on the T-orbit reference datasets**, so adding them there would be dead weight (the `separable-path-fork` dataset is the deliberate exception — a path-vs-fork collision that *does* earn `max-degree`). Two axes are therefore implemented but held **opt-in**: `Position` (radial-only) earns its place on radial-position contradictions (the corner-dot case), and `MaxDegree` (`Δ`, all shapes) earns its place on a path-vs-fork collision — the rigorous fix for the `V − E` path reading (above). Minimality is *per dataset*: the optimal chain is the smallest set of group-invariants that reaches the twin floor for the data at hand, so an opt-in axis is added exactly when a collision earns it (and the diagnostic, §3, says which). Lowering the floor itself is impossible ([§10.4](appendices/B-design-rationale.md)). With a per-sector `ChainPolicy` ([§4](02-core-model.md)) it becomes minimality *per sector as well* — a coordinate can be earned in one symmetry class and dead weight in another, and the chain no longer has to pay for it everywhere.
