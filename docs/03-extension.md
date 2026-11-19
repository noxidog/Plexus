# Self-Extension

*A new shape coordinate where different shapes collide.*

---

## 4. Self-extension

A mixed node means one of two things:

- **One shape, different labels by pose or place.** Upright T's labelled 1 and side T's labelled 0; a dot in the corner labelled 1 and the same dot in the centre labelled 0. The trailing levels (orientation, pose, corner) already separate these. Nothing is added.
- **Different shapes that share the key so far.** A path and a fork with the same box, symmetry, mass and joins. What is missing is a **shape** coordinate.

Self-extension runs at the end of `fit` and after every `accept`; `extend()` runs it on demand. Every junction (the node just above the trailing levels) that holds both labels is checked by comparing its members' canonical poses. If they are several shapes, the battery primitive with the best information gain, and not already on the path, is inserted at the junction, and the subtree is rebuilt from the stored grids. A primitive is only inserted if its gain is positive. This repeats until no junction changes. `new Plexus(rows, cols, lattices, false)` turns the automatic runs off.

### 4.1 The battery

`Primitive.BATTERY` holds properties of the shape's cell graph on the lattice that described it, the same adjacency as its joins (4 or 8 neighbours square, 6 hex):

| Primitive | Value |
|---|---|
| `max-degree` | the most neighbours any cell has |
| `branch-points` | cells with 3 or more neighbours |
| `end-points` | cells with exactly 1 neighbour |
| `pieces` | connected components |
| `loops` | independent cycles: joins − mass + pieces |

None of them refers to a direction, so each is unchanged by every turn and mirror of its lattice and is a legal coordinate on both tries. Ties go to the earlier primitive in the list. On `separable-path-fork-5x5` Plexus inserts `max-degree`: the fork has a cell with 3 neighbours, the path does not.

### 4.2 Diagnose

`Plexus.diagnose()` (`[8]` in the interpreter) lists the mixed junctions and the mixed leaves, each with its path and counts, and what resolves it. Mixed nodes elsewhere (above a junction, or between it and the leaves) are not listed. With nothing to list it prints `none: every node is pure`.

| Report | Where | Meaning |
|---|---|---|
| `one shape: separated by orientation, pose or corner` | junction | the trailing levels split the labels |
| `several shapes: separated by <primitive>` | junction | an extension was inserted here |
| `several shapes: no primitive separates them; kept as members at the leaves` | junction | the battery has nothing with positive gain |
| `the same grid trained with both labels` | leaf | a true contradiction; a read answers with the grid's majority label, and a tie falls to the leaf's majority |
| `different shapes with one full key: each stored grid answers with its own label` | leaf | shapes that agree on every level down to the corner, and that no primitive separated |
