package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.ops.Searcher;
import org.tervel.plexus.symmetry.Shape;
import org.tervel.plexus.symmetry.Transform;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Query by an <b>exact grid</b> — the typed placement, taken verbatim (no centering, no posing). Where
 * {@link StabilizerQuery} canonicalizes a frame-free shape, this preserves translation, so it can address
 * a specific placed grid — in particular a planted contradiction, which lives at a particular translation
 * that the centered pose can't reach. It resolves the label and the surviving/broken verdict for that
 * exact grid: for an exception, the broken set is the transforms that carry it onto a genuine, oppositely-
 * labelled grid.
 */
public class GridQuery implements Report {

    private final int[] grid;

    public GridQuery(int[] grid) { this.grid = grid.clone(); }

    @Override
    public String apply(Plexus plexus) {
        final var group = plexus.group();
        final var key = plexus.descriptor(grid);
        final var ctx = plexus.context(grid);
        final var ans = plexus.score(grid);
        final var side = (int) Math.round(Math.sqrt(grid.length));

        final var sb = new StringBuilder();
        sb.append("exact grid:\n");
        for (var r = 0; r < side; r++) {
            sb.append("    ");
            for (var c = 0; c < side; c++) sb.append(grid[r * side + c] == 1 ? 'x' : '.');
            sb.append('\n');
        }
        final var shape = Shape.of(grid, group);
        sb.append("  symmetry: ").append(key.radial() ? "(radial)" : "[" + group.describe(key.symmetry()) + "]")
          .append("   placement stabilizer: [").append(group.describe(group.stabilizer(grid))).append("]\n");
        sb.append(String.format("  shape:    %dx%d   isotropy %s%n", shape.height(), shape.width(), shape.isotropyFraction()));
        sb.append("  break:  ").append(plexus.coordString(key)).append('\n');
        if (ctx == null) {
            sb.append("  label:  unseen break -> reject (0)\n");
        } else {
            sb.append(String.format("  label:  answer=%d (%s)  pos=%d neg=%d%s%n",
                    ctx.answer(), ctx.isTarget() ? "TARGET" : "reject", ctx.pos(), ctx.neg(),
                    ctx.isException(grid) ? "  [this grid is an exception -> score " + ans + "]" : ""));
        }
        sb.append("  does each symmetry break the label?\n");
        sb.append("    surviving (keep ").append(ans).append("): ").append(names(new Searcher(grid, ans).apply(plexus))).append('\n');
        sb.append("    broken    (flip):    ").append(names(new Searcher(grid, 1 - ans).apply(plexus))).append('\n');
        return sb.toString();
    }

    private static String names(Iterator<Transform> it) {
        final var out = new ArrayList<String>();
        it.forEachRemaining(t -> out.add(t.label()));
        return out.isEmpty() ? "(none)" : String.join(", ", out);
    }
}
