# Appendix F — Worked Examples

*Examples carried end to end.*

---

## Worked example — the T and its decompositions

The T-tetromino is the clean case. Its canonical stabilizer is a **single mirror** through the centre column (`|Stab| = 2`, isotropy `|Stab|/|G| = 2/8 = 1/4`, orbit size 4). Watch that mirror act on the four cells:

```
x x x          fixed locus  = { centre, stem }         (on the mirror axis)
. x .          free orbit   = { left-arm, right-arm }  (swapped by the mirror)
```

The same shape admits a *lattice* of decompositions; the stabilizer is what ranks them:

| decomposition | pieces | description cost | verdict |
|---|---|---|---|
| **whole T** (no split) | `{T}` | the whole shape, no reuse | trivially coarse — nothing folded |
| **all mass pieces** | `{dot, dot, dot, dot}` | trivial atoms, but the **assembly map carries the entire shape** | trivially fine — the structure you dissolved reappears as gluing bits |
| **structural cut** (bar/dot, [§3](../01-descriptor.md) degree) | `{bar "xxx", dot centre, bar "x" stem}` | three atoms, junction named | irreducible *structurally*, but blind to the mirror |
| **stabilizer fold** (optimal) | `{arm, centre, stem}` + "apply the mirror" | **one** arm stored; the mirror regenerates the other | the minimal dictionary — the orbit partition |

Reading the rows: the two extremes are both bad, and for the *same* reason expressed two ways. *All mass pieces* throws the stabilizer away — every relation it would have given for free ("these two arms are one atom") must be re-encoded by hand in the assembly map, so you pay in gluing exactly what `Stab` was offering. *The whole T* leaves `Stab` nothing to act on. The right split is the only one that lets the stabilizer do its job: **as many pieces as `Stab` has orbits, no more, no fewer.**

---

## Register of worked examples

The examples that stay with the mechanism they illustrate, rather than being
collected here. Each is measured, and most are pinned by a test.

| example | shown in |
|---|---|
| The T and its decompositions under both cut-laws | this appendix, above |
| Per-sector chains on `renormalization-path-fork-9x9`, withholding `max-degree` below orbit 4 | [Sectors: the two tracks generalized](../01-descriptor.md) |
| The self-extension memory table across all six square datasets | [Self-extension](../02-trie.md) |
| The trie prefix census on `rect-bar-2x4` | [The chain as a trie](../02-trie.md) |
| The reference datasets, one slice-question each | [0. The task](../design.md) · `data/README.md` |
| The path-vs-fork collider at 9×9 under a `k = 3` fold | [Change of scale (renormalization)](A-mathematical-background.md) |
| The T-tetromino orbit as the canonical class | [0. The task](../design.md) |
