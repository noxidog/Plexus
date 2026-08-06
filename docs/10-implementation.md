# Implementation Status and Known Limits

*The open engineering TODOs, in one register.*

---

<a id="todo-domain-scope"></a>

## TODO — domain scope

> **TODO — domain scope.** Still **square-only**: everything that infers its frame from the cell count (`sqrt(length)`) instead of reading it off the Plexus, and so measures a non-square grid wrongly though it does not crash. That is the geometry-heavy set — `Shape`, `Fiber`, `Scale`, `CellGraph`, `Decompose`, `ThreadVolume`, `Plexus.center`, the REPL's typed-grid parsing, `CollisionDiagnostic`, and the trie's **self-extending** configuration (its orbit test and `Primitive` battery ride `Shape` and `CellGraph`) — and every consumer sitting on one of them inherits the limit: `[1] StabilizerQuery`, `[7] PossibilityReport`, `[v] VolumeReport`, `[9] atoms`, `[s] scale`. Both **opt-in** invariants are square-only for the same reason (`MaxDegree` and `Position` call `Invariant.side`); the shipped chain is not, because `Connectivity` carries an explicit frame. Multi-valued cells, hexagonal/aperiodic covers, 3-D, and non-grid domains need a different `SymmetryGroup`/lattice and remain unbuilt ([§2](04-symmetry.md), `Crystallographic`).

---

<a id="todo-covering-domains"></a>

## TODO — covering domains beyond the rectangle

> **TODO — covering domains beyond the rectangle.** Two groups are implemented: `Dihedral.d4()` (square) and `Dihedral.d2()` (rectangle — the transpose-free subgroup, the first realized non-square domain). The remaining **covering** orders — triangle (D3) and hexagon (D6), `Crystallographic.covers` — need a new lattice (the geometry-heavy ops still assume a square `int[]`); the **non-covering** orders — pentagon and beyond, aperiodic/Penrose — provably cannot be a finite `SymmetryGroup` at all (no finite group, no translation to quotient) and belong at the `Scale`/cut-and-project seam. Both are unbuilt, with the boundary, the `φ(n) ≤ 2` criterion, and the reasoning in `Crystallographic`. The injection seam is in place; the alternatives are not.

---

<a id="todo-seen-contexts"></a>

## TODO — slices are over seen contexts

> **TODO — slices are over seen contexts.** The query slices walk the `Topology` automaton, which iterates the contexts training *populated*. When the data covers the whole space that is every valid context; on partial data — including the shipped minimal datasets ([§0](design.md)), which carry only positives plus the colliding negatives — breaks/contexts never seen are missed. Generating them (the coarse stabilizer-fiber, or `Fiber` per partial key) would close the gap, mirroring the `seen < fiber` gap `possibility` already surfaces.
