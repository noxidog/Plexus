# Datasets — one question each

Every dataset fixes something about the shapes so that training on it and running one command asks a specific question of Plexus. Naming is `<question>-<shape>-<size>`. Run any with:

```
java -cp out org.tervel.plexus.cli.Main data/<file>
```

The startup line reports the tries' size and any extension inserted; the commands in the table are how to ask each question ([docs §6](../docs/05-cli.md)).

## Datasets are minimal by construction

An unseen key answers UNSEEN, so a sea of negatives adds nothing. Each file carries the positive class plus, where a question needs it, the few negatives that share a path with a positive: a contradiction only exists when the opposing label lands on the same node.

| dataset | records | question it asks | ask it with |
|---|---|---|---|
| `conservation-t-orbit-3x3.txt` | 8 (all +) | All eight T's (four orientations, two placements each): do they share one shape path? | `[t]`: one path down to joins, split only at orientation, pose and corner |
| `twin-t-flip-3x3.txt` | 8 (7 + / 1 −) | One T placement labelled 0: does it need a new coordinate, or is it the same shape in another place? | `[8]`: one shape, separated by orientation, pose or corner; `[5]` reads all 8 back |
| `position-corner-dot-3x3.txt` | 9 (1 + / 8 −) | A dot is 1 only in the top-left corner: does position survive a pose-free key? | `[8]`: one shape, separated by orientation, pose or corner (here the corner: `[t]` shows nine corner leaves). A dot's box is 1×1 and it has more symmetry on hex (12) than on squares (8), so it files under D6 |
| `separable-path-fork-5x5.txt` | 2 (1 + / 1 −) | A fork and a path with the same box (2×4), no symmetry, mass 5 and joins 4: which coordinate tells them apart? | startup line and `[8]`: `max-degree` inserted |
| `entropy-fiber-gap-5x5.txt` | 2 (all +) | A class seen only twice: how many grids does Plexus answer for beyond them? | `[s]` with label 1 under each setting: 2 confirmed, and 0, 94, 190, then 766 assumed as the setting widens |
| `renormalization-path-fork-9x9.txt` | 12 (6 + / 6 −) | Fork against path in a 9×9 frame, both 2×3 with mass 4 and joins 3: does the key separate them without help? | `[t]`: they part at the symmetry walk (the fork has the short-axis mirror, the path does not), so no extension is inserted; `[5]` reads all 12 back |
| `rect-bar-2x4.txt` | 8 (3 + / 5 −) | A non-square 2×4 frame: 1×3 bars are 1; a dot, dominoes, the 1×4 bar and a 2×2 block are 0 | `[t]`: one box per shape size (1×2, 1×3, 1×4, 2×2), and the dot under D6 |
