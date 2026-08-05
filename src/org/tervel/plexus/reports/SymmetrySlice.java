package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.Plexus.Key;
import org.tervel.plexus.ops.Fiber;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The slice taken along <b>surviving symmetry</b> rather than along a coordinate value. You name the
 * symmetries you want <em>kept</em> — a partial stabilizer — and this lists every full descriptor whose
 * context preserves at least those, then regenerates the shapes living there.
 *
 * <p>{@link SliceReport} fixes an invariant column ({@code sig=167}) and varies the rest; this fixes the
 * <b>symmetry</b> column and varies the invariants, which is the other half of the same slice-question
 * (§8): <i>hold something constant, enumerate what moves</i>. The match is subgroup <b>containment</b>,
 * {@code (stabilizer & required) == required}, so a partial request is a legitimate under-specification —
 * ask for {@code mirrorH} and you get every context that keeps it, whatever else it also keeps.
 *
 * <h2>Read the vocabulary off the header, not off the group</h2>
 * Contexts store the <b>rotation-canonical</b> stabilizer (§2) — the minimum mask over the shape's rotation
 * orbit — so which <em>name</em> a mirror wears may already have been folded away, and by how much depends
 * on the group:
 * <ul>
 *   <li><b>D4 (square).</b> {@code rot90} conjugation swaps {@code mirrorH}↔{@code mirrorV} and
 *       {@code diagMain}↔{@code diagAnti}, so each pair is a single rotation-conjugacy class and the
 *       minimum always spells it with the lower bit — {@code mirrorH}, {@code diagMain}. Asking for
 *       {@code mirrorV} alone matches nothing: not because no shape keeps that mirror, but because no
 *       <em>canonical</em> mask is written that way.</li>
 *   <li><b>D2 (rectangle).</b> The only rotation is {@code rot180}, which <em>commutes</em> with both
 *       mirrors, so nothing is folded and {@code mirrorH} and {@code mirrorV} remain separately askable.
 *       This is §11's "the rectangle" in miniature: breaking the transpose is exactly what makes the two
 *       axes distinguishable, here visible as a query vocabulary that is strictly richer than the square's.</li>
 * </ul>
 * Rather than make the caller derive that, the report prints the stabilizers actually present as its
 * header: the query vocabulary is whatever the trained table spells.
 *
 * <p>The radial track carries no stabilizer ({@code symmetry() == 0}, round shapes have no orientation), so
 * radial contexts appear only when nothing is required — the honest reading of "which symmetries survive?"
 * for a shape that has no orientation to break.
 */
public final class SymmetrySlice implements Report {

    private final int required;
    private final int side;
    private final int maxShapes;

    /** Slice on the symmetries that must survive; {@code maxShapes} caps the regeneration per context. */
    public SymmetrySlice(int required, int side, int maxShapes) {
        this.required = required;
        this.side = side;
        this.maxShapes = maxShapes;
    }

    @Override
    public String apply(Plexus plexus) {
        final var group = plexus.group();
        final var sb = new StringBuilder("contexts whose surviving symmetry ⊇ [")
                .append(group.describe(required)).append("]\n");

        // The vocabulary: which canonical stabilizers the trained table actually spells, and how often.
        final var vocab = new LinkedHashMap<Integer, Integer>();
        for (final var k : plexus.contexts().keySet())
            if (!k.radial()) vocab.merge(k.symmetry(), 1, Integer::sum);
        sb.append("  stabilizers present (the query vocabulary — canonical spellings):\n");
        if (vocab.isEmpty()) sb.append("    (none — every context is radial)\n");
        for (final var e : vocab.entrySet())
            sb.append(String.format("    [%s]  ×%d%n", group.describe(e.getKey()), e.getValue()));

        // Fiber infers a square frame from the cell count (the documented square-only limit of the
        // geometry-heavy ops), so on a rectangular domain it enumerates the wrong frame and comes back
        // empty. Say so rather than let an empty fiber read as "nothing lives here".
        final var probe = new int[Math.max(1, side * side)];
        final var square = plexus.rows(probe) == plexus.cols(probe);
        if (!square)
            sb.append("  note: this domain is rectangular; Fiber assumes a square frame, so the "
                    + "regeneration below is unavailable (routing and counts are unaffected).\n");

        // A stabilizer is a subgroup, so requiring a set of symmetries implicitly requires everything they
        // generate. Close the request rather than let the implication surface as a mystifying empty result.
        final var effective = required == 0 ? 0 : group.closure(required);
        if (effective != required)
            sb.append(String.format("  note: keeping [%s] forces [%s] as well (closure) — matching on [%s].%n",
                    group.describe(required), group.describe(effective & ~required), group.describe(effective)));

        final var matches = new ArrayList<Key>();
        for (final var k : plexus.contexts().keySet())
            if ((k.symmetry() & effective) == effective) matches.add(k);

        sb.append(String.format("%n  %d matching context(s)%n", matches.size()));
        if (matches.isEmpty() && effective != 0) sb.append(explainEmpty(plexus, effective));
        for (final var key : matches) {
            final var ctx = plexus.contexts().get(key);
            final var sym = key.radial() ? "(radial)" : "[" + group.describe(key.symmetry()) + "]";
            sb.append(String.format("%n    %s %s  →  answer=%d (%s)  pos=%d neg=%d  exceptions=%d%n",
                    sym, plexus.coordString(key), ctx.answer(), ctx.isTarget() ? "TARGET" : "reject",
                    ctx.pos(), ctx.neg(), ctx.exceptionCount()));

            // Regenerate: the descriptor run backwards. The fiber is structural, so it includes grids that
            // were never trained — the full set this descriptor admits, not the sample it was built from.
            if (!square) continue;
            final var fiber = new Fiber(key, side).apply(plexus).toList();
            sb.append(String.format("      fiber: %d structure(s) route here", fiber.size()));
            if (fiber.size() > maxShapes) sb.append(" (showing ").append(maxShapes).append(")");
            sb.append('\n');
            for (final var g : fiber.stream().limit(maxShapes).toList()) {
                Report.renderGrid(sb, "        ", g, plexus.rows(g), plexus.cols(g), 'x', '.');
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Why the slice came back empty — <b>deduced, never stored</b>. Three very different facts otherwise
     * print as the same "0 matching contexts": the group forbids it, canonicalization renames it, or the
     * data simply lacks it. The first two are pure group algebra on the requested mask (they do not read
     * the table at all); the third is what remains. Because the distinction is a derivation, materializing
     * it as structure — one trie level per symmetry bit, say — would only be caching a computation.
     */
    private static String explainEmpty(Plexus plexus, int effective) {
        final var group = plexus.group();
        if (!group.isSubgroup(effective))                       // unreachable after closure; kept as the guard
            return "    IMPOSSIBLE — [" + group.describe(effective) + "] is not closed under composition, so "
                    + "it is not a subgroup and no shape can have it as a stabilizer.\n";
        final var canon = group.canonicalMask(effective);
        if (canon != effective)
            return "    DEAD SPELLING — shapes with this symmetry exist, but contexts store the "
                    + "rotation-canonical mask, which always spells this class [" + group.describe(canon)
                    + "]. Ask for that instead.\n";
        return "    UNPOPULATED — legal, canonically spelled, and askable; this dataset simply contains no "
                + "shape that keeps it.\n";
    }

    /** The distinct canonical stabilizers the table spells, for a caller that wants the vocabulary alone. */
    public static List<Integer> vocabulary(Plexus plexus) {
        return plexus.contexts().keySet().stream().filter(k -> !k.radial())
                .map(Key::symmetry).distinct().toList();
    }
}
