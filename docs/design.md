# Plexus: Design

![Plexus](../assets/plexus-logo.png)

*A symmetry-routed exact classifier: it files every shape by what it is, not where it sits.*

---

## 0. The task

The input is a **binary grid**: an `int[]` of `rows × cols` cells, each 0 or 1. Each grid carries a label, 1 if it belongs to a target class and 0 otherwise. The classes are defined by **shape**: a shape and its turns, mirror images and moves are usually one class, but the data decides that, shape by shape. Sometimes a shape's orientation or position *is* the class (a side T against an upright T, a dot in the corner against a dot in the centre), and the router has to keep those apart when, and only when, the labels say so.

The hard case is **contradiction**: two grids that agree on every coordinate the router reads but carry opposite labels. Plexus handles them exactly. Where the trailing coordinates (orientation, pose, corner) separate them, they do; where different shapes collide, a new shape coordinate is inserted if one of the battery's primitives tells them apart; and what is left (the same grid trained with both labels, or different shapes no primitive separates) stays as stored grids at the leaf, each answering with its own majority, never smoothed over.

## 1. What it is

Plexus is a **router, not a network**. It has no weights, no thresholds, no statistics. Every grid is described ([§2](01-descriptor.md)), filed under a key in one of two tries ([§3](02-trie.md)), and read back with one of four verdicts.

```
grid ──describe──▶ D4 root / D6 root → box → symmetry walk → mass → joins → [extensions] → orientation → pose → corner
```

| Part | What it does |
|---|---|
| [Descriptor](01-descriptor.md) | bounding box, lattice pick (square or hex), canonical pose, the symmetry walk's answers, mass, joins, and the trailing coordinates |
| [The trie](02-trie.md) | two tries, training, the four verdicts and the extrapolation setting |
| [Self-extension](03-extension.md) | a new shape coordinate where different shapes collide; `[8]` diagnose |
| [Slices](04-slices.md) | constraints per level, a callback per grid, constructive generation |
| [The interpreter](05-cli.md) | `cli.Main`: score, trie, grid, slice, diagnose |
| [The engine](06-engine.md) | lazy grids, templates in every pose, stages folded over a grid, for a domain layer to drive |

The unifying idea is the **slice**: fix some coordinates of the key and read what stays constant, or enumerate what varies. Classification runs the key forward (grid to answer); a slice runs it backward (constraints to every grid that answers so). The same tries serve both.
