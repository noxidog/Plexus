# Appendix D — Performance Notes

*Cost models: what laziness buys, and what the fiber enumeration actually scales in.*

---

<a id="path-length"></a>

## Path length = description length

**Path length = description length.** The lazy walk's evaluation count is a per-query meter of how much structure the answer depends on, and it stratifies the dataset's truth by **law-likeness**: a pure-stop at depth 0 is a law of the whole domain (the label depends on nothing about the shape); depth 1, a law of symmetry ("the stabilizer decides"); depth `k`, a label that factors through the `k`-quotient; full walk + member lookup, a residual **fact** whose shortest description is itself. Shorter path = coarser quotient = larger fiber covered per bit of memory = the better compression ([§11](A-mathematical-background.md), "the law is a compression") — and a prediction of **renormalization stability**, since a shallowly-resolved label cannot depend on the fine detail that coarse-graining destroys first ([§11](A-mathematical-background.md), change of scale). The epistemic caveat is the flip side: purity is a claim about the *sample*, not the space, so a short path is either a law or unsampled territory — indistinguishable from inside — and the shortest paths make the grandest claims on the thinnest per-shape evidence. What redeems the bet is the walk itself: every insert that passes through a pure node without flipping it re-tests the law for free.

---

<a id="fiber-cost"></a>

## The cost model of the fiber enumeration

The win is structural: the expensive factor (box patterns) **does not depend on `side`** — a `2×3` tetromino is a handful of patterns whether the grid is `3×3` or `50×50`; grid size survives only as the placement count. The result is **exact by construction** — every member shares the key's mass and box, so both box orientations over all placements cover the whole fiber and the descriptor filter admits nothing else.
