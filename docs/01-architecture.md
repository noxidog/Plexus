# Architecture: Operations and Packages

*The package layout, the Operation spine, and the separation of concerns.*

---

## 7. Operations and packages

`Operation<T> extends Function<Plexus, T>` is the spine: a unary function consuming a Plexus. It is a **naming** interface rather than a dispatch point — eighteen classes implement it (nine operations directly, nine reports through `Report`) and nothing accepts an `Operation<?>` as a parameter — so it earns its place by fixing the shape a new operation must take, not by enabling polymorphism. Reports are `Operation<String>` (`Report`); the two inverse directions are both Operations — the `Searcher` is `Operation<Iterator<Transform>>`, the `Fiber` is `Operation<Stream<int[]>>`.

```
org.tervel.plexus
├── Plexus              router + Key/Context (the classifier), Sector + ChainPolicy (per-symmetry-class chains, §4)
├── Operation<T>        Plexus → T
├── symmetry/           Transform, SymmetryGroup, Dihedral (d4 square / d2 rectangle), Crystallographic (covering split), Shape   (the principled core)
├── invariants/         Invariant, Mass+Connectivity composed into Signature, MaxDegree, Position  (injected refiners)
├── residual/           ResidualResolver (the boundary seam), ExactExceptions (deterministic default)
├── cli/                Main — the interpreter (parse, train, REPL). The only main() in the tree, and outside the library core: the CLI depends on the router, never the reverse (§8)
├── trie/               Trie (self-describing paths; lazy pure-stop reads, online walk-as-update writes), Axis, Member (Grid / Descriptor), Resolution, ExtensionPolicy (the collision decision, injected)  — §5
├── ops/                Searcher (symmetry sweep), Fiber (descriptor-fiber), ThreadVolume (the thread's volume = determinant), Decompose (shape→atoms), Scale (block-decimation / change-of-scale), PrimeFrame (frame arithmetic: prime = scale-rigid), Topology (the automaton)
├── diagnostics/        CollisionDiagnostic (auto invariant discovery), ChainCritic (vet a coordinate; extend-or-stop per collision), Information (entropy + information gain, shared with the trie), Primitive battery, CellGraph
├── reports/            Report, ScoreReport, TopologyReport, TrieReport (the store's prefix census), PossibilityReport, VolumeReport, PrimeReport, SliceReport, SymmetrySlice, StabilizerQuery, GridQuery
└── utility/            Utility (payoff), Strategy (safe↔risky dial, dowry = neutral), OptimalStop (secretary harness)  (§11, deciding without the volume)

test/                   the suite: group axioms, orbit-constancy, the context projection, online training,
                        the trie's two configurations, the critic's grades, and the two §11 experiments
                        (renormalization closure, the optimal-stopping dial) — asserted, not printed
```

Ten of the fifteen menu entries are first-class `Report`s — nine classes, since `[2]` and `[3]` are the same `SliceReport` under two predicates — and most of those are **consumers of one automaton**. The other five are not `Report`s at all, which is why the tally above is nine operations *and* nine reports rather than eighteen reports: `[9] atoms` and `[s] scale` are `Operation`s returning **records** (`Decompose.Decomposition`, `Scale.Coarsening`) that the CLI renders through their own `render()`, since a fold's result is structure before it is text; `[8] diagnose` and `[c] critic` are `Operation<String>`s that live in `diagnostics/` and so do not reach for the `reports/` marker interface; and `[t] trie` **cannot** be one — a `Report` is a function of a trained `Plexus`, and `TrieReport.census` takes a `Trie`. Same shape, different subject. The entries sort onto the **same two axes as the menu** ([§8](09-api.md)) — the classify/generate direction × the node/table scope:

- **Resolve → / the whole table:** `ScoreReport` judges the whole router (confusion / MCC).
- **Generate ← / the whole table:** `Topology` walks the context table and hands each `(symmetry-break, context)` node to an injected consumer, so `TopologyReport` (print counts), `PossibilityReport` (enumerate + label each node's descriptor-fiber), and `VolumeReport` (measure each fiber's volume five ways and check the orbit-stabilizer identity, [§11](appendices/A-mathematical-background.md)) are node-functions over the same walk. `CollisionDiagnostic` (find separable collisions, emit invariant blueprints) is the same scope but stands apart from the walk: it needs per-grid labels, which a context's counts don't carry, so it regroups the labelled examples by descriptor directly. `ChainCritic` ([§3](03-invariants.md)) shares that regrouping and adds the ablation pass — drop each coordinate, re-derive the partition and the collision count — so it grades the chain rather than the data.
- **Generate ← / a node:** `SliceReport` filters that walk to a partial key — by invariant or by label; `SymmetrySlice` takes the complementary slice, fixing the **symmetry** column instead of a coordinate: name the symmetries that must *survive* (a partial stabilizer, matched by subgroup containment) and it lists every full descriptor that keeps them and regenerates each one's fiber; `Decompose` factors a single shape into its atoms (the decomposition direction, [§9](07-descriptors.md)), and `Scale` block-decimates a single grid into a coarser one (the change-of-scale direction, [§11](appendices/A-mathematical-background.md)) — its sibling fold.
- **Resolve → / a node:** `StabilizerQuery` poses a shape and lists its sibling breaks, or solves a stabilizer from a label; `GridQuery` resolves one exact placement.

The two inverse directions live apart as the operations behind the headline cells: `Searcher` is the symmetry sweep (broken/surviving), `Fiber` is the descriptor-fiber enumeration.

The split: **symmetry is the principled, group-theoretic core**; **invariants are swappable heuristics** whose only obligation is the invariance contract; **the chain policy owns which sector routes on what**; **the residual resolver owns what structure can't decide**; **the context owns its majority**; **the Plexus only routes**. `cli.Main` constructs `(group, invariants, resolver, data)` separately, so a dataset is loaded once and different invariant chains — or chain policies, or residual policies — tried on it.
