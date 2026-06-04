# Plexus

![Plexus](assets/plexus-logo.png)

**A symmetry-routed exact classifier — a router, not a network.**

Plexus has no weights, no gates, no thresholds, no statistics. It resolves a binary grid to a
label by *pure structural routing*: a symmetry verdict (the shape's stabilizer under the dihedral
group D4) refined by an injected chain of D4-invariant coordinates, addressing one **context** that
owns its answer (majority label, with the irreducible contradictions — the *twins* — left to an exact
exception store). Resolution is exact and discrete; the only memory is the exception set.

The unifying idea is the **slice**: fix some coordinates of the descriptor and read what stays
constant / enumerate what varies. Classification runs the descriptor forward; generation runs it
backward (enumerate a context's fiber). The same move, read two ways.

## Quickstart

Compile to `out/`, then run the interpreter on any dataset:

```sh
# compile (macOS/Linux)
javac -d out $(find src -name "*.java")

# train + drop into the REPL
java -cp out org.tervel.plexus.Main data/conservation-t-orbit-3x3.txt
```

The REPL menu is a 2-D grid — classify/generate (rows) by node/whole-table (columns):

```
                 a node                                   the whole table
  resolve  ->  [1] stabilizer  [4] grid                   [5] score
  generate <-  [2] invariant   [3] label  [9] atoms  [s] scale   [6] topology  [7] possibility  [8] diagnose
  [q] quit
```

Run the two-scale renormalization experiment (does a structural law survive coarse-graining?):

```sh
java -cp out org.tervel.plexus.RenormalizationLoopRunner
```

## Where to read more

- **[PLEXUS_DESIGN.md](PLEXUS_DESIGN.md)** — the full design: the symmetry core, the invariant chain,
  the residual boundary, the two inverse operations, decomposition, and the "frames as physics" reading
  (treating the machine's structure as a coherence argument for analyzing the world).
- **[data/README.md](data/README.md)** — the datasets, each posing one slice-question (conservation,
  twin, position, separable, entropy, renormalization) and how to ask it from the menu.
