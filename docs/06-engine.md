# The Engine

*Lazy grids, templates matched in every pose, and stages folded over a grid: what a domain layer drives.*

---

## 7. The engine

The router classifies one grid. The engine runs a **program over a grid**: a caller (a domain layer such as a screen grammar) describes its problem as planes, templates and stages, and the engine evaluates it. Nothing here knows what a label means; the caller's vocabulary says.

### 7.1 Grid

A `Grid` is rows × cols cells, each carrying one **label**, supplied by an `Oracle` the caller gives:

```java
Grid g = Grid.of(rows, cols, vocabulary, (r, c) -> recognize(r, c));
```

- **Lazy.** A cell's label is computed the first time something needs it and remembered. A cell nothing asks about is never computed. `recognized()` says how many were.
- **Immutable.** `relabel(r, c, label)` and `mark(plane, r, c, note)` return a new grid. Every grid derived from one capture shares what the oracle answered, so no cell is computed twice.
- **Planes.** `in(plane, r, c)` resolves a plane name through the `Vocabulary`, in order: a plane that holds everywhere; a plane a stage marked the cell in; the cell's label; a class of labels. Off the grid only the vocabulary's `outside` planes hold. The first two answer without computing the label.
- **Read and written stay apart.** A label is either **read** (the oracle observed it) or a **state** (a stage concluded it); the vocabulary's `states` names the second kind. `read(r, c)` is always the oracle's answer and `label(r, c)` the current one. `relabel` writes only states, and an oracle answering with a state is an error, so a conclusion can never pass for an observation.

| Vocabulary part | Meaning |
|---|---|
| `classes` | class name → the labels it groups |
| `values` | label → the integer it carries (`value(r, c)`) |
| `outside` | plane names that hold for a cell off the grid |
| `everywhere` | plane names that hold for every cell on the grid |
| `states` | labels only a stage writes and the oracle never answers |

### 7.2 Template

A `Template` is a small pattern of named cells around an **anchor**:

```java
Template t = Template.of(new String[][]{
        {"clear",   "clear",   "clear"},
        {"one",     "two",     "one"},
        {"covered", "covered", "covered"}}, 2, 1);   // anchor: row 2, column 1
```

- **Cropped by construction.** It is stored as offsets from the anchor; `null` cells are not part of it, so its size is whatever it was declared with. `box()` gives its extent.
- **Poses.** Its images under the square's eight motions about the anchor, the anchor staying put. Images that coincide are kept once, so a symmetric template has |D4| ÷ |Stab| poses. Each pose records the `move` that produced it from the declaration. The smallest pose is `canonical()`; two templates are equal when their canonical poses are, so a template and its rotation are the same template.
- **Containment.** `matches(grid, r, c)` is true when, in some pose with the anchor on `(r, c)`, every cell is in the plane it names. Cells it does not name are never looked at, so the grid may carry more. The first cell that fails rejects the pose.
- **Placements.** `covering(grid, r, c, role)` lists every placement that holds and has a cell named `role` on `(r, c)`: the ways the template can sit with that cell inside it. `Placement.count(grid, role, plane)` counts how many of its `role` cells are in `plane`, and `at(plane)` gives where its cells named `plane` landed.

### 7.3 Expression

`Expression.parse("value - flag")`: integers and names joined by `+` and `-`. `eval(name -> …)` takes what each name is worth from the caller.

### 7.4 Stages and pipelines

A `Stage<C>` is a context and a grid in, a grid out. The output may be the input unchanged; the caller assumes nothing and hands it on. The context `C` is the caller's: whatever it resolved before this grid existed. Plexus never looks inside it, and hands the same one to every stage, test and effect of one application.

```java
Stage<Run> forced = Stage.atEach("covered", (run, in, r, c) -> …condition…, (run, out, r, c) -> out.relabel(r, c, "flag"));
Pipeline<Run> solve = Stage.of(List.of(forced, freed, guess.onlyIf((run, g) -> !g.anyMarked("click"))));
Grid result = solve.apply(run, grid);
```

- `Stage.atEach(anchor, test, effect)` visits every cell of the `anchor` plane. Each test reads the grid the stage was **given**, so the order cells are visited in cannot change the result.
- `Stage.sweep(anchor, test, effect)` visits the same cells but **writes through**: each test reads the grid as the stage has written it so far, so a change is seen by every cell visited after it, and a cell an earlier change took out of `anchor` is skipped.
- `onlyIf(predicate)` runs a stage only when the predicate holds for its context and input.
- A `Pipeline` folds the grid through its stages in order, once, in one context. A pipeline is itself a stage.

### 7.5 Frames and shape counts

- `Frame.stabilizer(grid, rows, cols)` gives the motions of a **fixed frame** that leave a grid of any integers unchanged, as a mask over `Frame.moves(rows, cols)` (eight on a square frame, four on a rectangle). Unlike a descriptor it compares cell values in place.
- `Descriptor.stabilizerSize()` and `orbitSize()` count a **shape's** own symmetry wherever it sits: a domino has 4 motions and 2 orientations.

### 7.6 Repeats

`Repeat.of(grid, rows, cols)` gives the **whole shifts** that map a grid of cells onto itself, as `Descriptor` gives the turns and mirrors. The grid is binary or labelled: 0 is background, any other value a label. A picture of pixels is read into cells first (more than 64 labels is refused); repeats at a fractional number of pixels are the reader's business, not Plexus's. Everything is exact.

- **Agreement** at a shift is the share of labelled cells whose cell one shift over holds the same label, counted exactly (through Plexus's own FFT, rounded). **Prominence** is how far that stands from chance (how often the labels would agree if they were scattered independently) towards perfect: a repeat is a peak at a prominence of at least a quarter, relative because a pattern filling a third of the frame agrees far less overall than one filling all of it, and random cells agree as often as they are dense. A whole multiple of a shorter repeat is taken as the shorter one when every multiple of that shorter step up to it is at least half as prominent: a short shift that agrees only because cells clump together fails somewhere along that chain.
- **Every cell.** A one-cell step sits beside the zero shift, which agrees perfectly, so it is never a peak. Where no peak is found, the grid repeats every cell when one label clearly fills it (more than half the frame and more than twice any other label): the motif is that one label and everything else is a defect. A board read into cells is this case: the covered cell is the motif, the opened cells the defects, grouped by face. A lone shape in an empty frame, or random cells, does not repeat.
- **The basis** `v1`, `v2` is reduced to the shortest pair, `v1` pointing right and `v2` down; when they are equally long `v1` is the more horizontal; on a hex lattice `v2` is the neighbour at 60°. So the same pattern always gets the same `(i, j)` addresses.
- **The type** is read from exact lengths and angles on the grid's storage: the square grid's own, or for `Repeat.of(grid, rows, cols, Lattice.HEX_6)` the hex plane's, where a hex lattice is exact. `SQUARE`, `HEX`, `RECTANGULAR`, `RHOMBIC`, `OBLIQUE`, or `STRIPE` (one direction) and `NONE`. `moves()` lists the moves that map it onto itself, as Plexus's own D4 and D6 in their own order: all 8 for square, 12 for hex, `identity, rot180, mirrorH, mirrorV` for rectangular, `identity, rot180, diagMain, diagAnti` for rhombic, `identity, rot180` for oblique. `lattice()` is `HEX_6` for hex, `SQUARE_4` for the rest.
- `Repeat.basis(v1, v2, storage, …)` builds one from known steps; `as(type)` reads it as a declared type, which wins over what was measured.

`repeat.periods(grid, rows, cols, sizes, picker)` splits the grid into a **frame and its defects**:

- **A period** is the `|v1 × v2|` cells the repeat's parallelogram covers from a corner.
- **Where a period starts** is not in the repeat: every one of its corners tiles the grid. Each corner is ranked by how many of the repeat's moves leave the motif unchanged about the centre of the period it starts, and the most symmetric are the candidates (`phases()`). A tile's corner and the crossing of the gaps between tiles can be equally symmetric, so the caller's `Picker` chooses; `NEAREST_TOP_LEFT` when nothing tells them apart.
- **The region** is a block of whole periods, each scored by the share of its cells holding the motif's label. With `sizes` the block of each allowed size that stands highest above the ring of periods round it wins, so the size is exact; without, the largest connected block of the high group, the groups split where the data splits (only when the split explains at least 78% of the spread, above the 64% of noise round one value and the 75% of evenly spread values).
- **The motif** is the most frequent label at each period cell over the region (a first one, to rank the corners, over the periods that agree with their neighbours). **A defect** is a period that differs from it anywhere, and defects holding exactly the same labels share a group. `score` and `distance` (differing cells) are returned for every period.
