# API: the Interpreter

*The REPL surface — every query, on the two axes the descriptor itself has.*

---

## 8. The interpreter

`cli.Main <data-file>` loads the dataset (path from the argument), infers the grid **frame** (`rows × cols`) from the `nxm` header — routing square domains under D4 and rectangular ones under D2 — trains, and drops into a REPL:

```
               a node                                  the whole table
  resolve  ->  [1] stabilizer  [4] grid                [5] score
  generate <-  [2] invariant   [3] label  [b] symmetry  [9] atoms  [s] scale  [p] prime    [6] topology  [t] trie  [7] possibility  [v] volume  [8] diagnose  [c] critic
  [q] quit
```

The menu is laid out on the **two axes the descriptor itself has** — not a flat list:

- **Rows — the classify/generate duality ([§10.5](appendices/B-design-rationale.md), the Feynman two-directions of one map).** *Resolve* runs the map forward: route an input and read its verdict (`by grid` and `by stabilizer` read a label / a survive-vs-break partition; `score` reads the whole router's verdict). *Generate* runs it backward: pin coordinates and enumerate what lives there (`by invariant` / `by label` enumerate the matching contexts; `topology` / `possibility` / `diagnose` enumerate over the whole table; the node folds `atoms` and `scale` invert a *single* object — the decomposition and change-of-scale directions of the same [§9](07-descriptors.md) fold).
- **Columns — the scope ([§10.7](appendices/B-design-rationale.md)).** *A node* fixes a partial (or full) key; *the whole table* ranges over every context. `topology` is the corner where the two meet — the whole-table image of the by-key queries, i.e. **the empty partial key**.

So the headline `by stabilizer` query sits in the resolve/node cell as the **sum-over-paths** readout (allowed vs forbidden moves), and `topology` sits in the generate/table cell as "pin nothing, enumerate everything." Reading down a column or across a row is reading the same descriptor one axis at a time. The descriptor is a table whose columns are the symmetry verdict plus one per invariant — with the default chain that is a single `sig` column (the `Signature` packing mass and connectivity; the bounding box is *not* a column, [§3](03-invariants.md)), and `quadrant` only when `Position` is added for radial shapes. A query pins some columns and the node-consumer reads what's there. The entries below are grouped by the **same two cells as the grid**, not by number:

---

### Resolve → (run the map forward, read a verdict)

*A node* — pin a key, read its answer:

- **[1] by stabilizer** — *the headline path*. Every readout is framed as a **sum-over-paths** (Feynman): each move is a possible path, the moves that keep the input's label are the **allowed paths** (stay the target), the rest are **forbidden** (the label dies), and the solved group / position-class is the **history class** the input sums over. It has three modes counted by readout — posed, solve-oriented, solve-radial (`StabilizerQuery` itself counts two, by constructor: posed and solve, with solve branching per track; the counts are coprime because the tree is uneven — posed is a leaf, solve forks):

    - **Posed** — specify a **canonical shape + allowed symmetries**. The allowed symmetries do double duty: they name the stabilizer to query under **and orient** the canonical shape (a canonical T + `mirrorV` = the vertically-standing T). That pins two of the three dimensions (symmetry and shape/break); the consumer answers the single-axis terminal question on the third — *does each symmetry survive or break the label?* (the `Searcher` sweep) — and lists the sibling breaks under that stabilizer.
    - **Solve — oriented shapes** — leave the symmetries blank, give a **label**; the stabilizer is *solved from the label* on the **D4 subgroup lattice**. The orbit is swept against the **raw dataset labels** (not the Plexus's majority verdict, which would hide the planted contradictions) and split into surviving (image keeps the label), broken (image flips it), and unseen (never in data). **gather** climbs to the largest label-pure *subgroup* (the version-space general boundary — maximal recall); **separate** drops to the shape's own geometric stabilizer (the specific boundary). The gather search is **exhaustive, not greedy**, because a set of label-pure transforms need not be closed (two surviving mirrors can compose to a broken rotation). When the full surviving set is *not* itself a subgroup, gather does **not** keep it as a loose set of generators — that would be an invalid fold; instead it returns the **largest subgroup wholly contained in the surviving set** (the largest closed subset over all `2^|G|` subsets that lie inside the surviving mask and contain the identity). Recall is therefore capped at the biggest *valid* fold the labels permit, and in the worst case — no non-trivial surviving transform composes back inside the set — it floors at the **trivial group `{e}`** (identity alone, always a subgroup). The fold group is reported directly with **no pose step** — a label-fold group need not be any orientation's geometric stabilizer (the all-`1` T-orbit folds under full D4, which no T pose fixes), and the broken set is exactly where a contradiction caps the climb.
    - **Solve — radial shapes** (the circular group — dots, plus) — a radial shape has no orientation, so symmetry says nothing and the routing knob is **Position**: the centre-of-mass **quadrant**, one of nine (the 3×3 coarse grid `top-left … centre … bottom-right`). Solve walks the **position lattice** instead of the subgroup lattice, by exact placement:

        1. **Sweep.** Crop the shape to its bounding-box patch `h×w` and slide it over every placement in the frame — `top ∈ [0, side−h]`, `left ∈ [0, side−w]` — embedding each into a full grid.
        2. **Label by quadrant.** Read each placement's **raw dataset label keyed by exact placement** (not the Plexus: with no `Position` in the default chain the router folds all placements into one radial context — the collapse this branch exists to undo — so it goes to ground truth), and aggregate the observed labels into that placement's centre-of-mass quadrant.
        3. **Split.** A quadrant is an **allowed path** (survives) iff *every* label observed in it is the wanted label (its label-set is exactly `{label}`); a quadrant carrying any other label is **forbidden** (the position dies); a quadrant no labelled placement ever lands in is **unseen** and dropped.

      **Terminal conditions** (the position analogue of gather/separate — the two boundaries of the version space):
        - **gather** (general boundary, maximal recall) — the **union of all label-pure quadrants**: the geometric **shape-fiber**, i.e. recall the shape at every position that keeps the label. Reported as "sums over *N* allowed position(s) as one history."
        - **separate** (specific boundary) — pin to the **seed quadrant**, the single quadrant the centred input itself occupies (`Position` of the centred shape).

      This is where the dot-position contradictions live: on `position-corner-dot-3x3`, gather on label `1` returns exactly `top-left`.

    Blank symmetries *and* blank label default to the shape's own stabilizer.
- **[4] by grid** — the *exact typed placement*, verbatim (no centering, no posing). Where [1] canonicalizes a frame-free shape, this preserves translation, so it can address a specific placed grid — in particular a **planted contradiction**, which lives at a translation the centered pose can't reach. Resolves the label and the surviving/broken verdict; for an exception the broken set is exactly the transforms carrying it onto a genuine, oppositely-labelled grid. (`GridQuery`)

*The whole table* — judge every context forward:

- **[5] score** — confusion / accuracy / MCC. (`ScoreReport`)

---

### Generate ← (run the map backward, enumerate what lives there)

*A node* — pin a partial key, enumerate the matching contexts:

- **[2] by invariant** — pin one break column by its `name()` (`sig=167`, or `quadrant=0` when `Position` is in the chain); list the matching contexts. Mass and connectivity are packed inside `sig`, so they are queried through it, not as separate columns. (`SliceReport`)
- **[3] by label** — pin the answer (`1`=TARGET / `0`=reject); list the matching contexts. (`SliceReport`)
- **[b] by symmetry** — the complement of [2]: pin the **symmetry** column instead of a coordinate. Name the symmetries that must *survive* (a partial stabilizer) and it lists every full descriptor whose context keeps them, then regenerates each one's fiber. The match is subgroup **containment** (`stab & required == required`), so an under-specified request is legitimate — ask for `mirrorH` and you get everything that keeps it, whatever else it also keeps. A non-closed request is **repaired**, not rejected: a stabilizer is a subgroup, so keeping `mirrorH` and `mirrorV` necessarily keeps `rot180`, and the report closes the mask and says so. An empty result is explained rather than left bare — *impossible* / *dead spelling* / *unpopulated* ([§2](04-symmetry.md)). The header prints the stabilizers actually present, because canonical spelling is not guessable from the group alone. Regeneration is square-frame only (`Fiber`), and the report says so on a rectangular domain rather than showing a misleading zero. (`SymmetrySlice`)
- **[9] atoms** — *factor a typed shape into atoms* ([§9](07-descriptors.md)): cut its cell-graph into strands at the junctions under a chosen cut-law (`straight` = bars, `bent` = Ls), fold congruent strands into a dictionary of distinct canonical shapes, and report each atom's isotropy and the shape's stabilizer. Unlike [2]/[3] it doesn't slice the context table — it inverts a *single* shape into its parts (the decomposition direction, the sibling of [7]'s fiber enumeration). The T yields `bar + dot` under `straight`, `L + dot` under `bent`. (`Decompose`)
- **[s] scale** — *coarse-grain a typed grid by `k×k` block-decimation* ([§11](appendices/A-mathematical-background.md), the change-of-scale knob): fold each block to its canonical pose, keep one coarse bit, and read off what the fold discarded — `|orbit|` microstates per block, `Σ log|orbit|` integrated out. It is the **same [§9](07-descriptors.md) fold as [9] atoms**, sat one cell over in the same generate/node row: atoms cut *one shape* into finer parts, scale folds *one grid* into a coarser grid — both invert a single object rather than slicing the context table. So it rides the menu's two existing axes as a node fold, while *conceptually* being the third knob of [§11](appendices/A-mathematical-background.md) (conservation / relativity / scale). The two-scale renormalization *loop* that runs the flow over it lives in the suite as `RenormalizationTest` ([§11](appendices/A-mathematical-background.md)). (`Scale`)
- **[p] prime** — *is the `n`-cell frame itself prime?* — `[s] scale`'s arithmetic sibling. It measures the **translational** action the router quotients away (cropping to the box, [§6](06-search.md)): the cyclic group `Z_n`, whose subgroup lattice *is* the divisor lattice of `n`. So **prime ⟺ `Z_n` has no proper subgroup ⟺ the frame has no sub-period ⟺ no `k | n` with `1 < k < n` ⟺ `Scale` cannot coarse-grain it** — a prime is a **scale-rigid** observation window. The exact verdict is the orbit law: act with `Z_n` on every length-`n` binary row, and *every non-constant orbit has size `n`* iff `n` is prime (orbit size = minimal period, which divides `n`); the report also prints the **Fermat corollary** `2^n ≡ 2 (mod n)` — flagged a *proxy* (necessary, not sufficient: pseudoprimes pass it, the orbit test is exact, the [§3](03-invariants.md) bounding-box-vs-`Signature` distinction again). Unlike the other entries this is a **frame-level** diagnostic — a property of `n`, not of any context — so it sits beside `scale` rather than on the classify/generate × node/table grid. The *distribution* of primes is **not** reported: that spacing law is the analytic residual the symmetry points at but does not derive ([§5](02-core-model.md), and the [§11](appendices/A-mathematical-background.md) caveat). (`PrimeFrame`, `PrimeReport`)

*The whole table* — enumerate over every context:

- **[t] trie** — the **store's own shape**, which the context view cannot show: contexts are the *leaves*, while the store also holds every prefix. Nodes per rung, how many are already label-pure, and how many are **redundant** (their parent was pure, so that rung's coordinate decided nothing for them and a radix merge would collapse them upward). This is the compression the prefixes expose. (`TrieReport`)
- **[6] topology** — the full table via the `Topology` automaton (the empty partial key — pin nothing): contexts grouped by symmetry then coordinates, with imbalance, exception count, broken symmetries. (`TopologyReport`)
- **[7] possibility** — for every node, enumerate the descriptor-fiber (all grids that route there, incl. unseen) and split by predicted label; `seen` vs `fiber` exposes what training never covered. (`PossibilityReport`)
- **[v] volume** — for every node, **measure** the descriptor-fiber rather than list it: its volume five ways — `|fiber|` (full-frame count), `|orbit|` (the bbox-frame rigid orbit), `[G:Stab]` (the orbit-stabilizer index — the group determinant), `√det(J Jᵀ)` (the Jacobian/coarea determinant of the invariant-component gradients), and `det L` (the Matrix-Tree spanning-tree determinant). The `id` column checks the exact identity `|orbit| = [G:Stab]` ([§11](appendices/A-mathematical-background.md), *thread = determinant*); `|fiber|/|orbit|` is the translational multiplicity. (`VolumeReport`, `ThreadVolume`)
- **[8] diagnose** — the collision diagnostic ([§3](03-invariants.md)): for every entropy>0 context, classify the collision as an irreducible symmetry-twin or a separable one, and for the separable ones emit the information-gain-maximising topological primitive as the blueprint for the next invariant. Also runs automatically as a training post-pass. (`CollisionDiagnostic`)
- **[c] critic** — the diagnostic aimed at the **chain** rather than the data ([§3](03-invariants.md)). Grades every coordinate *legal / useful / new / earned / not cheating* — `ILLEGAL`, `INERT`, `REDUNDANT`, `UNEARNED`, `DEGENERATE`, with `ORIENTING` as the non-fault verdict for the documented radial-only `Position` exception — using an ablation pass that drops each coordinate and re-derives both the partition and the collision count. Then, per collision, answers *extend or stop*: `IRREDUCIBLE` (same orbit — the chain is not the problem, stop looking), battery-has-it, or battery-empty, in which case it renders both sides as shapes for the reader. Every grade but `ILLEGAL` is relative to the loaded dataset. (`ChainCritic`)

The generate/node entries [2]/[3] and the headline [1] are all **partial keys into the same context table** — by symmetry, by an invariant column, by label — so `topology` (generate/table) is just the empty partial key. The `{stabilizer, break}` distinction dissolves into "which columns you pin"; the resolve/generate row is *which way you read them*.

