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
public final class GridQuery implements Report {

    private final int[] grid;

    public GridQuery(int[] grid) { this.grid = grid.clone(); }

    @Override
    public String apply(Plexus plexus) {
        final var group = plexus.group();
        final var key = plexus.descriptor(grid);
        final var ctx = plexus.context(grid);
        final var ans = plexus.score(grid);

        final var sb = new StringBuilder();
        sb.append("exact grid:\n");
        Report.renderGrid(sb, "    ", grid, plexus.rows(grid), plexus.cols(grid), 'x', '.');
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
        // The three verdicts partition the group exactly, so every transform is accounted for — including
        // the ones that carry the grid off the trained map, which a 0/1 sweep would file under "surviving".
        final var self = plexus.verdict(grid);
        sb.append("  does each symmetry break the label?  (surviving = same verdict, broken = flipped)\n");
        for (final var v : Plexus.Verdict.values())
            sb.append(String.format("    -> %-9s %s%s%n", v, names(new Searcher(grid, v).apply(plexus)),
                    v == self ? "   <- surviving (the input's own verdict)" : ""));
        return sb.toString();
    }

    private static String names(Iterator<Transform> it) {
        final var out = new ArrayList<String>();
        it.forEachRemaining(t -> out.add(t.label()));
        return out.isEmpty() ? "(none)" : String.join(", ", out);
    }
}
