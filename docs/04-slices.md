# Slices

*Constraints per level, a callback per grid, and constructive generation.*

---

## 5. Slices

A slice walks one root or both under a constraint per level and streams every matching grid to a callback.

```java
plexus.slice(
    Slice.on(Root.D4)                      // Root.D4, Root.D6, or Root.BOTH
         .box(2, 3)                        // omit for any box
         .atLeast("short-axis mirror")     // or .exactly(...)
         .mass(4, 5)                       // a range; omit for any
         .joins(3, 4)
         .label(1)                         // omit for both labels
         .orientation(0).pose(0)           // placement constraints; omit for any
         .corner(0, 0)
         .per(Slice.Per.PLACEMENT)         // the default; or POSE, or SHAPE
         .extrapolate(Extrapolate.SYMMETRY),  // the default
    (grid, context, evidence) -> {
        // grid:     the shape, posed and placed in the input frame
        // context:  the node that controls its label: path, counts, answer, root, depth, boundary
        // evidence: CONFIRMED or ASSUMED
    });
```

### 5.1 Constraints

- **Box:** short × long on D4 (order does not matter), or the extent on D6.
- **Symmetries follow the walk.** A required symmetry takes only the yes edge; an unmentioned one takes both; `exactly` takes the no edge for every symmetry not listed (list a closed set: two perpendicular mirrors bring rot180). Symmetry names belong to a kind of box: `short-axis mirror` to rectangles, `mirrorH` to square boxes, `mirror30` to hex. A required name the box's walk does not have rules that box out, so most names pick a kind of box and with it a root. `rot180` is in all three walks: `atLeast("rot180")` over `Root.BOTH` returns rectangles, square boxes and hex shapes alike. Names are matched without regard to case, spaces or hyphens (`long-axis-mirror` means `long-axis mirror`); a name no walk asks about throws `IllegalArgumentException` listing the names, which `Slice.symmetryNames()` also returns.
- **Mass, joins:** ranges.
- **Label:** 1, 0, or both.
- **Orientation, pose, corner:** the placement levels. Orientation is 0 or 1 (on a rectangle: not turned or turned; on hex: rows shifted right or left); a square box has none, so an orientation constraint rules square boxes out. Pose is the pose code, with 0 the canonical pose itself. Corner is the row and column of the box's top-left in the input frame.
- **Per:** what one delivered entry stands for ([§5.2](#52-what-comes-back)).
- **Extrapolate:** how far the slice may go beyond what was trained ([§3.4](02-trie.md)).

### 5.2 What comes back

- **CONFIRMED:** every trained grid on a matching path, once each, delivered as found, with its leaf as the context. The label constraint is checked against the answer a read of that grid gives.
- **ASSUMED:** every grid a pure node within the setting stands in for, and whose label the constraint keeps, delivered after the walk: the deepest node first, and at equal depth the nodes whose parent is mixed first, since those sit next to disagreement and are the best to check.

`Slice.Result` counts the confirmed and assumed entries and the nodes skipped as too large to generate.

By default every matching grid is an entry (`Per.PLACEMENT`). `Per.POSE` folds the positions together and delivers one grid per distinct pose of each shape; `Per.SHAPE` also folds the poses and delivers one grid per distinct shape. The grid delivered for a group is a trained one if the group has any (so the entry is CONFIRMED), and among those the one nearest the canonical pose and the frame's top-left: not turned, lowest pose code, lowest corner. On the eight T's of `conservation-t-orbit-3x3`, with label 1 and `Extrapolate.NONE`: 8 placements, 4 poses, 1 shape, the last shown as `xxx/.x.` in the top-left corner. Folded entries arrive together after the walk.

### 5.3 Constructive generation

For a stand-in at a shape level (symmetry, mass, joins, extension), candidates are built, not searched for:

1. Read the box and the symmetries already answered yes on the path.
2. Group the box's cells into the orbits of those symmetries.
3. Fill whole orbits only: any shape with those symmetries has each orbit all filled or all empty.
4. Expand each candidate over every pose and placement in the input frame, and keep the grids whose read would break off at exactly this node, and which the setting and the query admit.

A one-mirror 2×3 node has 4 orbits, so 2⁴ fills instead of 2⁶; a fully symmetric 3×3 node has 3, so 2³ instead of 2⁹. For stand-ins at the trailing levels, the candidates are every pose and placement of the shapes stored under the node. On the hex root the candidates are all subsets of the s×s square input box, without the orbit step. A node whose enumeration would exceed 2²¹ is skipped and counted in `Slice.Result`: more than 21 orbits on the square root, or a box larger than 4×4 on the hex root.

What a slice generates is exactly what reading answers for. The suite checks this on three 3×3 datasets, for every setting and both labels, against all 512 grids of the frame.
