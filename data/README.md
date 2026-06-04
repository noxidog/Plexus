# Datasets — one slice-question each

Every dataset here is a **slice**: it fixes some coordinates of the descriptor so that
loading it and running one menu op asks a specific question of the router (the §11
"physics as slices" reading). Naming is `<question>-<shape>-<size>`. Run any with:

```
java -cp out org.tervel.plexus.Main data/<file>
```

The startup post-pass prints the collision diagnostic; the menu cells in parentheses
below are how to ask each question interactively (§8).

## Datasets are minimal by construction

The router defaults every **unseen** key to reject (label 0) and a context owns its label
by **majority**, so the sea of negatives is redundant — it is just the default. Each file
therefore carries **only the positive class**, plus, for the *collision* datasets, the
**few negatives that share a context with a positive** (a contradiction cannot be expressed
by default-reject — the opposing label must actually land in the same context). That is the
whole asset: no background fill.

| dataset | records | slice fixed | question it asks | ask it with |
|---|---|---|---|---|
| `conservation-t-orbit-3x3.txt` | 8 (all +) | the symmetry (the full T orbit, no contradiction) | *Hold the symmetry still — what is the conserved identity?* | `[1] stabilizer` (solve from label 1 → the fold group), `[6] topology` |
| `twin-t-flip-3x3.txt` | 8 (7 + / 1 −) | one orbit member flipped | *Which moves keep me myself, and which kill the label? (the irreducible twin)* | startup `diagnose` → PROVABLE TWIN; `[1] stabilizer`, `[4] grid` on the flipped placement |
| `position-corner-dot-3x3.txt` | 9 (1 + / 8 −) | a dot at the top-left corner is 1, every other dot 0 | *When a shape has no orientation, what routes it? (the radial Position axis)* | startup `diagnose` → radial twin; `[1] stabilizer` radial solve (`gather` on 1 → `top-left`) |
| `separable-path-fork-5x5.txt` | 2 (1 + / 1 −) | a fork (Δ=3) vs a path (Δ=2) of equal (V,E) | *Does my chain need a new axis to tell these apart? (mint a coordinate)* | startup `diagnose` → emits the `max-degree` blueprint |
| `entropy-fiber-gap-5x5.txt` | 2 (all +) | a pure target class seen only twice | *Given all I can measure, how many distinct states stay indistinguishable? (entropy = log\|fiber\|)* | `[7] possibility` → `fiber=192 seen=2/192` |
| `renormalization-path-fork-9x9.txt` | 12 (6 + / 6 −) | path-vs-fork at side 9 (so `k=3` decimation is non-degenerate) | *Is the structural law the same at every scale, or does it shatter under coarse-graining?* | startup `diagnose` (fine-scale separable collision); full two-scale verdict via `RenormalizationLoopRunner` |

## The two kinds of question (§11)

- **Read-an-existing-coordinate** — conservation, twin, position, entropy: the slice cuts, and
  the answer is a value the descriptor already holds.
- **Mint-a-new-coordinate** — separable (`max-degree`) and renormalization (the fold raising the
  exception floor): the current chain can't resolve it, so the answer is a *new dimension of
  description* — the diagnostic emitting a blueprint. These are the "best questions": their
  answers grow the descriptor rather than reading it.

The full two-scale renormalization experiment (the 9×9 path-fork set synthesised at full size
in-code) runs with:

```
java -cp out org.tervel.plexus.RenormalizationLoopRunner
```
