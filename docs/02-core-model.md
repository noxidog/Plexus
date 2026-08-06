# Core Model: the Composite Key and the Context

*How a grid becomes a key, and how the context that key addresses decides.*

---

## 4. The composite key

The descriptor is one resolution, with no fixed key budget. The symmetry verdict is **not a column inside the tuple** — it is the **type** of the key. The root router branches on it once, into two strongly-typed tracks, each carrying only the coordinates that apply to it:

```java
Key descriptor(int[] g) {
    final var sector = sector(g);                    // (radial verdict, canonical stabilizer)
    final var coords = chainFor(sector).stream().map(d -> d.applyAsInt(g)).toList();
    return sector.radial() ? new RadialKey(coords) : new OrientedKey(sector.stabilizer(), coords);
}
```

```
               [ Input Grid ]
                     │
              [ Symmetry Core ]
                     │
          ┌──────────┴──────────┐
          ▼                     ▼
   [ RadialKey ]         [ OrientedKey ]
     ├── Position          ├── Signature (V, E)
     └── Signature         └── canonical stabilizer
```

`Key` is a **sealed interface** `permits RadialKey, OrientedKey`; each is a record, so value equality (record + `List.equals`) still makes it a `Map` key. Crucially, the two tracks are **decoupled in storage** — the store keeps a separate root per track and a lookup never crosses between them, so the two coordinate layouts never mix. This is what keeps each coordinate table **perfectly uniform within its track** — the radial-only `Position` column can no longer hang ragged off an oriented key, because oriented keys are a different type with no such field. The **default chain is a single `[Signature]`** — mass and connectivity packed into one invariant column. Whether that reaches the twin floor is a property of the dataset, not of the chain, and the reference sets split cleanly in two. On the T-orbit sets it **does**: `conservation-t-orbit` and `entropy-fiber-gap` seal zero exceptions, and `twin-t-flip` and `position-corner-dot` seal exactly one each, which the diagnostic proves is a provable twin ([§3](03-invariants.md)) — memory forced, not memory wasted. On the path-fork sets it **does not**: `separable-path-fork-5x5` seals one and `renormalization-path-fork-9x9` seals five, and every one of them is *reducible* (zero twins, §5), so the chain is simply too short — adding the opt-in `MaxDegree` closes it, and self-extension (§5) closes it automatically. So the single-`Signature` chain is minimal for the T-orbit sets and under-provisioned for the path-fork ones, which is precisely the per-dataset reading of minimality ([§3](03-invariants.md)). `Position` is an **opt-in** axis (radial-only); the corner-dot exception is at the floor for *this* chain, but it is the one twin verdict with a way out ([§3](08-diagnostics.md), the critic) — the context is radial, so an orientation-scoped axis can still split it, and adding `Position` is how you take it. Two shapes route to the same context iff they are the same track **and** agree on **every** applicable axis. (Reports read keys through the shared `radial()` / `symmetry()` / `coords()` accessors, so the whole-table walk is unchanged; `symmetry()` is 0 on the radial track, which has no orientation.)

---

### Sectors: the two tracks generalized

The radial/oriented split is not really about *values* — it is about **vocabulary**. Radial drops the stabilizer column and admits `Position`, which is illegal on the oriented track: different columns, not different numbers. Once you see it that way, two tracks looks like an arbitrary stopping point, and orbit–stabilizer says why:

```
|orbit| = |G| / |Stab|
```

A coordinate is legal iff it is constant on an orbit ([§3](03-invariants.md)), so it must agree on `|G|/|Stab|` grids. **More symmetry ⇒ smaller orbit ⇒ weaker constraint ⇒ more coordinates become legal** — and at full symmetry the orbit is a single grid, where *any* function of it is trivially orbit-constant. The legal vocabulary grows monotonically up the subgroup lattice:

| stabilizer | orbit | what a coordinate must agree across |
|---|---|---|
| trivial | 8 | 8 grids — strictest |
| one mirror | 4 | 4 |
| Klein four | 2 | 2 |
| full D4 | 1 | nothing — anything is legal |

So the chain is selected per **`Sector`** — `(radial, canonicalStabilizer)` — by an injected **`ChainPolicy`**, a peer of the group, chain, and resolver. `ChainPolicy.uniform()` is the default (one chain, gated only by the radial verdict), and a strict special case: every reference measurement is unchanged under it.

Three things make it safe:

- **Sectors are unions of whole orbits.** Orbit-mates have conjugate stabilizers, hence the same canonical mask, hence the same sector and the same chain — selecting by sector can never shatter a class. *Measured: 0 shattering events over 96 (grid, transform) pairs.*
- **Every key determines its sector**, so `chainFor(key)` recovers the coordinates behind any stored key — which is what lets the reports, `ThreadVolume`, and the trie keep reading keys they did not construct.
- **Radial stays a single sector** on purpose. Subdividing it would need the stabilizer inside `RadialKey`, which drops it deliberately; without that column two radial sectors running different chains could emit equal coordinate lists and silently merge.

`ChainPolicy.byOrbit(group, maxOrbit, withheld)` is the shipped non-uniform example: it **withholds** coordinates from low-symmetry sectors rather than appending them to rich ones. Withholding matters twice — appending would list a declared coordinate twice in the rich sectors, and rebuilding by filtering the declared list preserves chain **order**, which is load-bearing because chain order is the trie's rung order (§5).

Measured on `renormalization-path-fork-9x9`, withholding `max-degree` below orbit 4:

```
[identity]             orbit=8   chain=[sig]        → sig=583           pos=5 neg=6   ← collision returns
[identity, mirrorH]    orbit=4   chain=[sig, deg]   → sig=583 deg=3     pos=1 neg=0
```

Note the honest cost: the trivial sector, deprived of `deg`, gets its collision back. A per-sector chain is not a free win — it is a decision about **where to spend coordinates**, and spending them only where symmetry makes them cheap means not spending them where the data needed them. Minimality ([§3](03-invariants.md)) is now a per-sector question rather than a global one.

The trie absorbs this without losing laziness, because the stabilizer is its first edge: by the time the walk needs an invariant, the stabilizer has already named the sector, so the chain is known exactly when it is first required. (An early pure-stop above that edge reports the declared depth as an upper bound — exact under `uniform`.)

---

## 5. The context: majority + exact exceptions

A `Context` is the **view a leaf projects**, and it owns its own decision:

```java
record Context(long[] counts, List<int[]> exceptions) {
    int     answer()        { return counts[1] > counts[0] ? 1 : 0; }   // majority label
    boolean isException(g)  { ... exact match among the dissenters ... }
}
```

It is materialised on demand rather than stored, and `exceptions` is **derived**: a member is one exactly when its own recorded majority contradicts its leaf's, both read from live counts (see *Why training is one pass*, below).

- `accept` counts each example into its context.
- `score` routes to the context, then delegates the leaf decision to the injected `ResidualResolver` (below). The default returns the context's **majority answer**, flipped iff the grid is a memorised exception.
- A context with no positive majority is a *reject*; an unseen key defaults to 0.

`score` collapses two different facts onto that `0` — *"routed to a context whose answer is negative"* and *"no example ever routed here"* — so `verdict` returns the three-state `Verdict` enum (`POSITIVE / NEGATIVE / UNSEEN`) instead, at the cost of nothing (same single lookup, it just declines to spend the reject signal). `score` remains for the scoring surface, which wants a bit. The distinction is not cosmetic: the `Searcher` ([§6](06-search.md)) sweeps the group testing each image against a target, and under `score` a `target = 0` sweep would count **never-explored** transforms among a grid's *surviving* symmetries — absence of evidence read as evidence of the label. Matching on `verdict` makes broken / surviving / unseen a genuine partition of the group.

The exceptions are the **pixel-level memory for whatever the chain could not resolve** — every grid whose label contradicts the majority of the context it routed to. Some of those contradictions are irreducible symmetry-twins (an oppositely-labelled shape sharing every symmetry-derived quantity, which no invariant can separate); others are merely collisions a richer chain *could* have split, and telling the two apart is exactly what the collision diagnostic ([§3](03-invariants.md)) is for. On `renormalization-path-fork-9x9` the default chain derives five exceptions and **zero** twins: the path/fork collision is separable, so those five are reducible and a deeper chain removes them, which is precisely what self-extension does ([§5](05-trie.md), the trie). An oppositely-labelled twin is a *different grid* and never matches, so a member can **never be over-flipped** — the exact match is robust by construction.

---

### Why training is one pass

An exception is a grid whose label contradicts its context's **converged** majority — a fact a *stored* exception list cannot know early: a grid fed while counts are immature would be falsely flagged, and a real exception fed early would be missed. A frozen list therefore forces two passes over the data: count, then seal.

That constraint belongs to the **storage, not the problem**. A leaf that keeps its members derives the same contradictions at read time — a member is an exception exactly when its own recorded majority contradicts its leaf's, both read from live counts — so a later insert that flips a majority flips every derived exception with it, atomically. Nothing is frozen, so nothing can go stale: `fit` is one pass and `accept` is legal at any point afterwards.

Deriving also answers the mixed-label grid correctly. A frozen list seals a grid seen under **both** labels once per contradicting *example*, which makes `isException` true and flips it away from the label it mostly carries — a grid 3:2 in favour of `1` answers `0`. Comparing majorities answers it `1`. (`TrainingTest` pins both the online property and this case.)

---

### The residual boundary: where routing ends and you brute-force or learn

Routing is **exact and deterministic up to the twin floor** ([§10.4](appendices/B-design-rationale.md)) — symmetry plus the invariant chain place every grid in a context by pure structure. But two symmetry-twins share *every* group-derived quantity by proof, so the group algebra has nothing left to say about which one is which. That is the **residual**: the incompressible signal outside the symmetry algebra ([§11](appendices/A-mathematical-background.md), "entropy = log|fiber|"). This marks the exact theoretical line where **structural routing reverts to memorisation** — and resolving the residual is no longer routing, it is whatever you bring to bear (an exact lookup, a brute-force search, a learned net).

The code makes that line a seam. A `Plexus` routes a grid to its `Context` — the most structure can do — then delegates the intra-context leaf decision to a **`ResidualResolver`** (`org.tervel.plexus.residual`). The resolver is **injected at construction**, a peer of the symmetry group and the invariant chain — `cli.Main` constructs all three and passes them in (`new Plexus(group, invariants, resolver)`); there is no hidden default. The one that ships is **`ExactExceptions`**, the deterministic floor described above: majority, flipped iff the grid is a memorised exception — exact on seen contradictions, never over-flips. A resolver only ever decides *within one already-routed context*, reached only after structure is exhausted, so it can never corrupt the deterministic routing — only fill the part the routing provably cannot. Injecting a brute-force or learned resolver instead (one that **predicts** the residual rather than memorising it, generalising to unseen twins) changes nothing structural; it only replaces this one leaf function.
