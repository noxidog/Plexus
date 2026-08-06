# Descriptors: Shape and Decomposition

*A shape as its own identity, and the stabilizer as the optimal factorization.*

---

## A shape as itself, and the two fibrations (`Shape`)

There are **two** inverse problems here, and keeping them apart is what makes the dots behave:

- **`D : grid → key`** — the classifier's descriptor (symmetry verdict + invariant coords).
- **`Shape.of : grid → shape`** — the *geometric* identity. It crops to the smallest bounding rectangle (translation quotiented away), folds to the group-**canonical pose** (orientation quotiented away), and tags the result with its **isotropy fraction** `|Stab|/|G|` — the relative size of the fundamental domain the shape folds into. By orbit–stabilizer that fraction is exactly `1/|orbit|`: `1/8` for a shape with no symmetry (the generic "one octant" floor under D4), `1/4` for a single mirror, `1` for full symmetry. ("Isotropy" is the group-theoretic name for the stabilizer; the fraction is the dimension-free generalization of "which octant" — nothing in it is tied to 8 or to 2D.)

A `Shape` is a complete **frame-free symmetry-address**: `(cells, height, width)` say *what it is up to symmetry*, `isotropy` says *how symmetric it is*. Both are orbit-invariants.

---

## How the two fibrations relate

For an **oriented** shape, its geometric fiber coincides with a single descriptor context — the orbit *is* the context (the T: 8 instances, one key). For a **radial** shape the descriptor keys on **position**, so it *splits* the shape across contexts: the dot is one fully-symmetric shape (`isotropy 1/1`, one canonical `1×1` pose) whose geometric fiber is **all 9 placements**, which the descriptor partitions into corner/edge/centre keys. So the shape-fiber would be the **union of descriptor-contexts over the shape's positions** — coarser than `D⁻¹` exactly where, and only where, an invariant is position-sensitive. This is why the two are kept distinct conceptually even though only `D⁻¹` is wired: a future "all instances of this shape" operation would be the geometric union, not a single context.

This is the realisation of "do the reference-frame work once": translation is factored out by cropping to the box (its canonical representative), the box shape reads out the symmetry ceiling, the canonical pose folds the orbit, and `isotropy` records what the fold discarded.

---

## 9. Decomposition: the stabilizer as the optimal factorization

Routing folds a shape *onto* its fundamental domain ([§2](04-symmetry.md)); **decomposition** is the same fold read the other way — *what are the irreducible pieces, and how do they reassemble?* The thesis of this section is that the answer is not a free parameter: a shape's own **stabilizer** dictates the optimal split.

---

### The stabilizer acts on the parts

A shape's stabilizer `Stab` — the symmetries it does **not** break (`canonicalStabilizer`, [§2](04-symmetry.md)) — is usually read as a *label* (how symmetric the shape is). But it is also an **operator on the shape's own cells**: it permutes them. Decomposition is just reading off that action.

> **The optimal decomposition is the orbit partition of the shape under its own stabilizer.** Store one representative per orbit; the elements of `Stab` replay the rest.

That is exactly the [§2](04-symmetry.md) fundamental-domain fold: the atom is the seed in the fundamental domain, `Stab` is the machine that unfolds it.

Acting on the cells, `Stab` sorts them into two roles:

- **fixed points** — cells on a symmetry axis, mapped to themselves: the **junction / dot** pieces, stored as-is (nothing to regenerate).
- **free orbits** — cells swapped among each other: the **repeated** pieces (bars, arms), stored *once* with `Stab` producing the copies.

So even the *seams* are the stabilizer talking: they fall between its fixed locus and its free orbits.

---

### How you arrive there: |Stab| is the compression ratio

This isn't an aesthetic choice — it is the **minimum-description-length** optimum (the codec reading of [§11](appendices/A-mathematical-background.md)), and the stabilizer is the leading term. The dictionary size is `parts / Stab-orbits`, and the bits a surviving symmetry saves on a part are `log₂|its orbit| = log₂(|Stab| / |Stab_part|)`. So **the bigger the unbroken stabilizer, the larger the orbits, the fewer distinct atoms**, and `Shape.isotropy = |Stab|/|G|` — already computed on every shape — *is* that compression ratio:

```
plus (+) : Stab = D4 (8)     → all 4 arms one orbit  → dictionary {arm, centre}      (max fold)
T        : Stab = mirror (2) → 2 arms one orbit       → dictionary {arm, centre, stem}
L / S    : Stab = {e} (1)     → no orbits merge        → dictionary = every part       (no fold)
```

The fully-symmetric plus pays for one arm; the asymmetric L pays for all of itself — and the gap is precisely `log|Stab|`. You don't *search blindly* for the optimum: `canonicalStabilizer` hands it to you, so the optimal split is a **readout of a quantity the router already computes**.

---

### What the stabilizer does *not* fix

One thing it leaves open: *how finely to describe the single stored seed* inside the fundamental domain. Is the seed a bar, an L, or a dot? That is the **cut-law** (the degree/curvature choice of [§3](03-invariants.md) — cut at branch points only, or also at corners), and it is orthogonal: the stabilizer sets **what is stored once versus regenerated**; the cut-law sets **how the one stored representative is itself written**. The optimal decomposition is the composition of the two — *structural cut, then stabilizer quotient* — and only the second half is determined by symmetry. This is the same division of labour as the rest of the system: the group is the principled core, the structural invariants are the swappable refiners ([§7](01-architecture.md)).

---

### The operation

`new Decompose(grid, law).apply(plexus)` (`org.tervel.plexus.ops.Decompose`, a sibling of `Searcher` and `Fiber`) runs this as the decomposition direction of the fibration. It walks the cell-graph from each endpoint, cutting into **strands at the junctions**, then **folds** congruent strands into a dictionary of distinct canonical `Shape`s with multiplicities — that fold is the §9 orbit collapse, and each entry carries its `isotropy`, so the dictionary size *is* the compression the stabilizer buys.

The bar-vs-L choice is the one parameter, a `CutLaw` ([§3](03-invariants.md)): `STRAIGHT` ends a strand at a corner (atoms are **bars**), `BENT` carries it through bends (atoms are **Ls / paths**). On the T:

```
[STRAIGHT]  atoms: BAR "xxx" (cells 0,1,2) + DOT "x" (cell 4)   → bar + dot
[BENT]      atoms: ELL ".x/xx" (cells 0,1,4) + DOT "x" (cell 2)  → L + dot
```

The stabilizer is reported alongside (`[identity, mirrorV]` for the T) as the assembly rule, and a maximally-symmetric shape like the plus folds its four arms into a single dictionary entry — the visible payoff of a larger `|Stab|`.
