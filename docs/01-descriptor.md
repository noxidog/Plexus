# The Descriptor

*Everything Plexus reads off one grid, computed once.*

---

## 2. The descriptor

`Descriptor.of(grid, rows, cols)` turns a grid into the values the key is built from. It works on the shape's own crop, so where the shape sits in the frame enters only at the last coordinate.

### 2.1 The bounding box

One pass over the pixels gives the box of the non-zero cells: a square (h = w) or a rectangle. An empty grid has no box and gets the single key `empty`.

The box caps the symmetry the shape can have. Any symmetry of a shape also maps its box onto itself, so it fixes the box's centre, and the centre is the only point the shape can be symmetric about. A rectangle allows at most D2 (rot180 and the two axis mirrors); a quarter turn or a diagonal mirror would turn an h × w box into w × h.

### 2.2 Picking the lattice

A shape lives on one of two lattices, and each has its own trie.

1. **Rectangle → square lattice, always.** The hex lattice is never tried on a rectangle. A 60° or 120° turn cycles the three hex axes, so a shape with one needs equal extents on all three, and its box on the square input is square.
2. **Square box → measure both diagonals.** The width across the anti-diagonal is the spread of row + column; across the main diagonal, the spread of row − column. If a width equals the side, shearing the rows makes that diagonal a hex neighbour direction:
   - row + column matches → shift the rows **right**: square cell `(r, c)` becomes axial `(q = c, r)`, and anti-diagonal neighbours become adjacent;
   - row − column matches → shift them **left** (mirror the columns, then shift right), and main-diagonal neighbours become adjacent;
   - both match → take both shifts and keep the one whose form up to D6 is smaller. Every pose of the shape then makes the same choice.
3. **Compare.** Hex wins only with strictly more symmetry than the square lattice gives the same shape; a tie stays square.

| Shape | Square symmetries | Hex symmetries | Lattice |
|---|---|---|---|
| plus `.x./xxx/.x.` | 8 | 4 | square |
| hex disk `.xx/xxx/xx.` | 4 | 12 | hex |
| solid 3×3 | 8 | (anti-diagonal width 5, not 3: not tried) | square |

The shift is one cell to one cell, nothing interpolated, so the input stays exact. The disk's 90° turn, `xx./xxx/.xx`, matches the other diagonal, shifts the other way, and lands on the same hexagon.

**Lattices.** A router is built on a set of lattices, fixed for its life, because the neighbours are part of the key: they define `joins` and every self-extension primitive. `Lattice.SQUARE_4` (edge neighbours), `Lattice.SQUARE_8` (edge or corner neighbours, the king's move) and `Lattice.HEX_6` (the six neighbours of the sheared reading). A legal set holds exactly one square lattice, since rectangles always file on the square lattice and square joins must have one meaning, and optionally `HEX_6`; anything else throws `IllegalArgumentException`. The default is `SQUARE_4` + `HEX_6`.

```java
new Plexus(rows, cols)                                    // SQUARE_4 + HEX_6, self-extension on
new Plexus(rows, cols, Lattice.SQUARE_8)                  // square only, 8 neighbours
new Plexus(rows, cols, Lattice.SQUARE_8, Lattice.HEX_6)   // 8-neighbour squares, hex still tried
new Plexus(rows, cols, Set.of(Lattice.SQUARE_4), false)   // square only, self-extension off
```

Without `HEX_6`, steps 2 and 3 are skipped for every square box: each shape stays on the square lattice, files under the D4 root, and the D6 root stays empty. The hex disk then reads as a square box with `rot180`, `diagMain` and `diagAnti`. `Descriptor.of(grid, rows, cols, lattices)` describes one grid the same way; every descriptor records the lattice that described it in `lattice()`.

### 2.3 The symmetry walk

The walk asks one question per symmetry, yes or no, in a fixed order: turns first, smallest angle first, then mirrors in the order the table gives (by axis angle on hex). A turn whose inverse is already asked (rot270 on D4, rot240 and rot300 on D6) always gets the same answer and is left out.

| Kind | Group | Questions |
|---|---|---|
| rectangle | D2 | rot180, short-axis mirror, long-axis mirror |
| square box | D4 | rot90, rot180, mirrorH, mirrorV, diagMain, diagAnti |
| hex | D6 | rot60, rot120, rot180, mirror0, mirror30, mirror60, mirror90, mirror120, mirror150 |

Symmetries combine (two perpendicular mirrors force rot180; rot90 forces rot180), so some answers are implied by earlier ones. Those levels simply have one child.

### 2.4 The canonical pose

The questions must not depend on how the shape arrived, so the walk is asked of a canonical pose:

- **Rectangle:** turned to landscape first, the *envelope*, then the smallest of its four D2 images. In landscape the short-axis mirror is the one that swaps the two ends of the long side; a T always has exactly that mirror, whichever way it arrived.
- **Square box and hex:** the pose whose walk answers say yes as early as possible in the order above, ties to the smallest image. A tall T is walked with its mirror as `mirrorH`, never `mirrorV`.

### 2.5 Invariants and trailing coordinates

On the canonical pose:

- **mass**: filled cells;
- **joins**: adjacent pairs on the shape's own lattice: 4 or 8 neighbours square (`SQUARE_4` or `SQUARE_8`), 6 hex.

Then the three trailing coordinates, which record how and where the shape arrived:

| Coordinate | Rectangle | Square box | Hex |
|---|---|---|---|
| orientation | envelope turn: 0 not turned, 1 turned | none (−1) | shift direction: 0 right, 1 left |
| pose | which of 4 D2 transforms reached the canonical pose (0–3) | which of 8 D4 transforms (0–7) | which of 12 D6 transforms (0–11) |
| corner | raw top-left corner of the box in the input frame | same | same |

The pose is the lowest-numbered transform that reaches the canonical pose. A symmetric shape reaches it by several, so only |group| ÷ |stabilizer| codes occur: 4 for an L on a rectangle, 2 for a T, 1 for a bar, the plus or the hex disk.

### 2.6 Companion planes

A grid often comes with other planes of the same frame: the other faces of a board, a mask of what is known. `descriptor.toCanonical(plane, ...)` moves them exactly as the shape was moved (cropped at its corner, enveloped or sheared, then turned or mirrored into the canonical pose) and returns a `Fold`.

```java
final var d = plexus.describe(ones);
final Fold fold = d.toCanonical(ones, twos, unknown);   // one motion for all three planes
fold.cells(1);                                           // the twos, as {a, b} in the canonical frame
```

- **One motion for every plane.** Where the planes sit relative to the shape and to each other is kept. Coordinates are those of the canonical pose, with the shape starting at (0, 0); companion cells can lie outside its box and be negative.
- **Symmetric shapes.** Several motions reach the canonical pose of a symmetric shape and each puts the companions somewhere different. All are tried and the smallest fold is kept. On hex, a shape that reads as the same hexagon sheared either way (a dot, the disk) is tried with both shears, because a quarter turn of the frame swaps the two diagonals.
- **Result.** Turning, mirroring or moving the whole frame never changes the fold. Two inputs fold equal exactly when one motion of the lattice carries the shape and every companion onto the other's.
- **Empty shape.** No canonical pose exists; the planes come back unmoved, in input coordinates.
