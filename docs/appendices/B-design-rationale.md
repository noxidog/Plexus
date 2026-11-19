# Appendix B — Design Rationale

*The principles the design answers to, and the reasoning behind the cut-law dial.*

---

## What the cut-law really chooses: where the variance lives

The cut-law is not a cosmetic choice between two pictures of the T — it decides **where the shape's irregularity is stored**, and the `isotropy` column is the tell:

- `STRAIGHT` → bar `xxx` (isotropy **1/2**) + dot (**1/1**): both atoms are *symmetry-rich*. A straight bar lies on a reflection axis and is fixed by a mirror; a dot is fixed by everything.
- `BENT` → L `.x/xx` (isotropy **1/4**) + dot: the L is *symmetry-poor* — the bend gives it a handedness, so most of the group moves it.

So forcing strands straight **forces every atom to be one of the maximally-symmetric primitives the group admits.** But the shape's asymmetry does not vanish — the total irregularity is **conserved across `atoms + assembly`** (the codec accounting of [§11](A-mathematical-background.md)). The cut-law only chooses *which side of that sum carries it*:

- `STRAIGHT` keeps the atoms symmetric and **expels the symmetry-breaking feature — the bend — into the assembly** (the junction relation, the relative placement). The dictionary stays clean and reusable; the gluing carries the variance.
- `BENT` **absorbs that same bend into the atom** (the L *is* the bend), leaving a trivial assembly. The variance moves into the dictionary.

This is the same symmetry-core-vs-structural-refiner split the whole machine runs on ([§7](../design.md)), surfacing in decomposition. `STRAIGHT` atoms are an **orthogonal basis** — few, axis-aligned, maximally reused across all shapes (bars and dots are everywhere), at the cost of a richer assembly to say how the symmetric parts bend together. `BENT` atoms are **bespoke** — expressive, shape-specific, barely reusable, but the shape is then a near-trivial pile of pieces. And it closes back onto the stabilizer thesis: `STRAIGHT` decomposes **along the shape's surviving symmetries** — every atom respects the mirror (the bar is symmetric about the axis, the stem sits on it) — so the cut never breaks a symmetry the shape still has, and the corner, the one place the structure departs from axis-alignment, is exactly what gets excised as the seam. `BENT` is willing to break that symmetry inside an atom in exchange for a smaller assembly. The dial, then, is: **symmetric reusable atoms + informative gluing, or expressive one-off atoms + trivial gluing** — same shape, same conserved total, different split.

---

## 10. Principles

1. **Route, don't over-teach.** Fundamentally different kinds of object (a plus, a T, a dot) belong in different contexts; one learner forced to separate them all just learns proxies.
2. **Each axis must be earned by a collision and invariant under the prior axes**, or it undoes them. Position is the exception that proves the rule (radial-only).
3. **Keep the one degeneracy you want — rotation of a genuine shape — and break the rest.** Telling them apart is the whole game.
4. **Symmetry-twins are irreducible by symmetry, by proof.** When two shapes share every symmetry quantity, the only remaining signal is outside the symmetry algebra; exact identity memory, quotiented by the surviving symmetry, is the honest floor.
5. **Classify and generate are the two directions of one map.** The descriptor is a fibration; a context is a router forward and a generator of its fiber backward.
6. **Do the reference-frame work once, and let the frame read out the symmetry.** Crop to the bounding box (factor out translation, the transform that was never in the group), and the box shape *is* the stabilizer ceiling — a rectangle has already disabled the off-diagonal symmetries as self-symmetries, keeping them only as orbit generators across the transpose. Normalize once at the front; routing and generation both run in the small frame.
7. **The descriptor is a nested coordinate system — stabilizer ⊃ break ⊃ label — and each operation fixes one coordinate and enumerates the next.** Fix a stabilizer, enumerate its breaks (by stabilizer). Fix a break, enumerate its grids (possibility / fiber). Fix the grid, and only the label remains (by grid). Every query is a partial key into the context table; topology is the empty key. Naming discipline: *generate* is the duality direction (principle 5); the operation is to *enumerate / invert a fiber* — say which one, the shape-fiber or the descriptor-fiber. The interpreter's menu ([§8](../05-cli.md)) is laid out on exactly these two axes — the classify/generate direction (principle 5) × the node/table scope (this principle) — so the UI mirrors the descriptor's own structure.
