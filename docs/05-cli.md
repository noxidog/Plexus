# The Interpreter

*`cli.Main`: train on a data file, then query.*

---

## 6. The interpreter

```
java -cp out org.tervel.plexus.cli.Main data/conservation-t-orbit-3x3.txt
java -cp out org.tervel.plexus.cli.Main data/conservation-t-orbit-3x3.txt --lattice square8
```

`--lattice` picks the router's lattices, comma separated from `square4`, `square8` and `hex6`; the default is `square4,hex6`. A set without exactly one square lattice is refused with the reason.

`Main` reads the data file, trains a `Plexus` on it (self-extension on), prints one line with the example count, the frame, the lattices, the number of trie nodes and any extensions inserted, and opens a REPL:

```
plexus: 2 example(s) in a 5x5 frame, lattices [SQUARE_4, HEX_6], 16 node(s), extensions [max-degree]
```

```
[5] score  [t] trie  [4] grid  [s] slice  [8] diagnose  [q] quit
```

| Command | What it does |
|---|---|
| `[5]` score | reads every training grid back under the default setting and counts those answered with their own label |
| `[t]` trie | draws both tries as indented paths, each node with its counts; leaves marked |
| `[4]` grid | reads a typed grid (`xxx/.x.`: rows separated by `/`, `x` or `1` filled, placed top-left in the frame): its descriptor, then its verdict under each setting with the controlling context |
| `[s]` slice | prompts for root, symmetries to keep at least, mass, joins, label, orientation (`0` / `1`, or `upright` / `turned`, `right` / `left`), pose (a number; 0 is the canonical pose), corner (`row,col`), what to show one entry per (`placement`, `pose` or `shape`), and extrapolation (`none`, `placement`, `symmetry` or `everything`), then prints every grid the slice produces with its evidence and context, and the confirmed and assumed counts. A blank answer, or `any`, `all`, `both` or `none`, leaves a prompt unconstrained. Mass and joins take a number or a range (`4-5`). Symmetry names ignore case, spaces and hyphens; an unknown name, a bad range, or an unknown root, label, orientation, pose, corner, show or extrapolation value is refused with a message saying what was expected |
| `[8]` diagnose | lists the mixed junctions and leaves and what resolves each ([§4.2](03-extension.md)) |

### 6.1 Data files

The first line is `nxm`. Only `n`, the row count, is read from it; the column count is the first record's cell count divided by `n`. Then one record per example: `n` rows of the grid (`x` filled, anything else empty), followed by a line holding the label, 0 or 1.

```
3x3
xxx
_x_
___
1
```
