package org.tervel.plexus.diagnostics;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.reports.Report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The <b>chain critic</b> — the diagnostic aimed at the chain rather than at the data. Where
 * {@link CollisionDiagnostic} asks <i>"is the chain deep enough?"</i> and emits a blueprint for the next
 * coordinate, this asks the two questions that come <em>before</em> accepting one:
 *
 * <ol>
 *   <li><b>Is each coordinate any good?</b> A coordinate can fail four independent ways, and only one of
 *       them (illegality) is about correctness — the rest are about earning its place (§3, minimality).</li>
 *   <li><b>At each collision, is extending even possible?</b> Separating <i>"your chain is too short"</i>
 *       from <i>"no chain can do this"</i> is what makes a diagnostic worth trusting: it says when
 *       <b>not</b> to bother.</li>
 * </ol>
 *
 * <h2>The four grades</h2>
 * <ul>
 *   <li>{@code ILLEGAL} — not constant on an orbit of the <em>oriented</em> track, so orbit-mates route to
 *       different contexts and the class shatters. This breaks the invariance contract
 *       ({@link Invariant#invariantUnder}) and is the only grade that is a correctness bug. (A radial-only
 *       coordinate that varies over an orbit is graded {@code ORIENTING} instead — that is the documented
 *       {@code Position} exception: radial shapes have no orientation, so a position-like axis is
 *       well-defined again exactly there.)</li>
 *   <li>{@code INERT} — one distinct value over the data. A column that never splits anything.</li>
 *   <li>{@code REDUNDANT} — dropping it leaves the induced partition <em>identical</em>: functionally
 *       dependent on the coordinates already present. (Dropping a coordinate can only merge contexts, never
 *       split them, so an unchanged partition count is exactly "it merged nothing".)</li>
 *   <li>{@code UNEARNED} — it does split contexts, but dropping it adds <b>no collisions</b>: it separates
 *       structures the labels never disagreed about. This is minimality (§3) made testable — the chain
 *       should carry a coordinate only when the collision diagnostic earns it.</li>
 *   <li>{@code DEGENERATE} — near-singleton buckets: the values are approaching an identity column, which
 *       scores a perfect information gain while generalising to nothing. Memorisation wearing a
 *       coordinate's clothes, and the pathology a greedy split-on-collision rule will walk into if a
 *       candidate is admitted on gain alone.</li>
 * </ul>
 *
 * Together they read: <i>legal, useful, new, earned, not cheating.</i> A candidate primitive should pass all
 * five before entering the chain — which is the point: vetting belongs <em>before</em> admission, not after.
 */
public final class ChainCritic implements Operation<String> {

    /** The verdict on one coordinate. {@code OK} and {@code ORIENTING} are passes; the rest are faults. */
    public enum Grade { OK, ORIENTING, ILLEGAL, INERT, REDUNDANT, UNEARNED, DEGENERATE }

    /**
     * One coordinate's report card. {@code distinct} is how many values it takes over the grids it applies
     * to, {@code avgBucket} the mean class size ({@code applicable / distinct} — the generalisation width,
     * heading to 1.0 for an identity column), {@code contextsLost} how many contexts merge when it is
     * dropped, and {@code collisionsAdded} how many fresh label collisions that merge creates — the number
     * that decides whether it was earned.
     */
    public record Critique(String name, Grade grade, int distinct, double avgBucket,
                           int contextsLost, int collisionsAdded, String detail) { }

    private final List<int[]> grids;
    private final List<Integer> labels;

    public ChainCritic(List<int[]> grids, List<Integer> labels) {
        this.grids = grids;
        this.labels = labels;
    }

    // ---- 1. the coordinate report cards -------------------------------------------------------

    /** Grade every coordinate in the chain against the dataset. */
    public List<Critique> critique(Plexus plexus) {
        final var baseContexts = partition(plexus, -1).size();
        final var baseCollisions = collisions(plexus, -1);
        final var out = new ArrayList<Critique>();

        for (var idx = 0; idx < plexus.invariants().size(); idx++) {
            final var inv = plexus.invariants().get(idx);
            // Applicability is per SECTOR, not merely per track: under a per-sector chain policy a declared
            // coordinate may route some symmetry classes and not others.
            final var applicable = grids.stream()
                    .filter(g -> plexus.chainFor(plexus.sector(g)).contains(inv)).toList();
            final var distinct = (int) applicable.stream().mapToInt(inv::applyAsInt).distinct().count();
            final var avgBucket = distinct == 0 ? 0 : (double) applicable.size() / distinct;

            final var reduced = partition(plexus, idx);
            final var contextsLost = baseContexts - reduced.size();
            final var collisionsAdded = collisions(plexus, idx) - baseCollisions;

            final var broken = firstOrbitBreak(plexus, inv, applicable);
            final Grade grade;
            final String detail;
            if (broken != null && !inv.applies(false)) {
                grade = Grade.ORIENTING;
                detail = "varies over an orbit (" + broken + ") but is radial-scoped — the documented "
                        + "Position exception (§3): radial shapes have no orientation to preserve.";
            } else if (broken != null) {
                grade = Grade.ILLEGAL;
                detail = "NOT orbit-constant on the oriented track (" + broken + ") — orbit-mates route to "
                        + "different contexts and the class shatters. Fix or remove.";
            } else if (distinct <= 1) {
                grade = Grade.INERT;
                detail = "one value over this dataset — it never splits anything here. Says the data is "
                        + "narrow (a single orbit routes by stabilizer alone), not that the axis is bad.";
            } else if (contextsLost == 0) {
                grade = Grade.REDUNDANT;
                detail = "dropping it merges no contexts — functionally dependent on the rest of the chain.";
            } else if (collisionsAdded == 0) {
                grade = Grade.UNEARNED;
                detail = "splits " + contextsLost + " context(s) but dropping it adds no collision — it "
                        + "separates structures the labels never disagreed about (minimality, §3).";
            } else if (avgBucket < 1.5 && applicable.size() >= 4) {
                grade = Grade.DEGENERATE;
                detail = String.format("mean class size %.2f — approaching an identity column: perfect gain, "
                        + "no generalisation.", avgBucket);
            } else {
                grade = Grade.OK;
                detail = String.format("legal, %d values, mean class %.2f; dropping it would cost %d "
                        + "collision(s).", distinct, avgBucket, collisionsAdded);
            }
            out.add(new Critique(inv.name(), grade, distinct, avgBucket, contextsLost, collisionsAdded, detail));
        }
        return out;
    }

    /**
     * The first {@code (grid, transform)} witness where {@code inv} changes value under a routing transform,
     * or null if it is orbit-constant everywhere it applies. This is {@link Invariant#invariantUnder} run as
     * a search for a counterexample rather than as an assertion.
     */
    private String firstOrbitBreak(Plexus plexus, Invariant inv, List<int[]> applicable) {
        for (final var g : applicable) {
            final int rows = plexus.rows(g), cols = plexus.cols(g);
            for (final var t : plexus.group().transforms())
                if (!inv.invariantUnder(t, g, rows, cols))
                    return "under " + t.label() + ": " + inv.applyAsInt(g)
                            + " → " + inv.applyAsInt(t.apply(g, rows, cols));
        }
        return null;
    }

    /** The contexts the chain induces, with chain position {@code omit} left out ({@code -1} = keep all). */
    private LinkedHashSet<List<Object>> partition(Plexus plexus, int omit) {
        final var out = new LinkedHashSet<List<Object>>();
        for (final var g : grids) out.add(keyOf(plexus, g, omit));
        return out;
    }

    /** How many induced contexts carry both labels, with chain position {@code omit} left out. */
    private int collisions(Plexus plexus, int omit) {
        final var byKey = new LinkedHashMap<List<Object>, List<Integer>>();
        for (var i = 0; i < grids.size(); i++)
            byKey.computeIfAbsent(keyOf(plexus, grids.get(i), omit), k -> new ArrayList<>()).add(labels.get(i));
        return (int) byKey.values().stream().filter(ls -> ls.stream().distinct().count() > 1).count();
    }

    /**
     * The routing key as a plain value list (so it compares by equality), minus the coordinate at chain
     * position {@code omit}. Positional, not by name: a chain may legitimately carry two coordinates with
     * the same {@link Invariant#name()}, and dropping both would misreport each as redundant.
     */
    private static List<Object> keyOf(Plexus plexus, int[] g, int omit) {
        final var sector = plexus.sector(g);
        final var parts = new ArrayList<Object>();
        parts.add(sector.radial());
        if (!sector.radial()) parts.add(sector.stabilizer());
        final var declared = plexus.invariants();
        final var active = plexus.chainFor(sector);
        for (var i = 0; i < declared.size(); i++)
            if (i != omit && active.contains(declared.get(i))) parts.add(declared.get(i).applyAsInt(g));
        return parts;
    }

    // ---- 2. per-collision: extend, or stop? ---------------------------------------------------

    @Override
    public String apply(Plexus plexus) {
        final var sb = new StringBuilder("Chain critic — is each coordinate any good, and can each collision be fixed?\n");

        sb.append("\n  coordinates (legal, useful, new, earned, not cheating)"
                + "  —  every grade but ILLEGAL is relative to THIS dataset\n");
        sb.append(String.format("    %-10s %-11s %7s %10s %8s %10s%n",
                "name", "grade", "values", "mean class", "splits", "costs"));
        for (final var c : critique(plexus))
            sb.append(String.format("    %-10s %-11s %7d %10.2f %8d %10d%n      %s%n",
                    c.name(), c.grade(), c.distinct(), c.avgBucket(), c.contextsLost(), c.collisionsAdded(), c.detail()));

        // Group by context; every mixed-label context is a collision the chain could not resolve.
        final var byKey = new LinkedHashMap<Plexus.Key, List<int[]>>();
        final var labelByKey = new LinkedHashMap<Plexus.Key, List<Integer>>();
        for (var i = 0; i < grids.size(); i++) {
            final var k = plexus.descriptor(grids.get(i));
            byKey.computeIfAbsent(k, x -> new ArrayList<>()).add(grids.get(i));
            labelByKey.computeIfAbsent(k, x -> new ArrayList<>()).add(labels.get(i));
        }

        sb.append("\n  collisions (extend, or stop?)\n");
        var any = false;
        for (final var key : byKey.keySet()) {
            final var ls = labelByKey.get(key);
            if (ls.stream().distinct().count() < 2) continue;
            any = true;
            final var members = byKey.get(key);
            final var pos = new ArrayList<int[]>();
            final var neg = new ArrayList<int[]>();
            for (var i = 0; i < members.size(); i++) (ls.get(i) == 1 ? pos : neg).add(members.get(i));

            final var sym = key.radial() ? "(radial)" : "[" + plexus.group().describe(key.symmetry()) + "]";
            sb.append(String.format("%n    context %s %s  —  %d pos / %d neg%n",
                    sym, plexus.coordString(key), pos.size(), neg.size()));

            if (CollisionDiagnostic.isProvableTwin(plexus, pos, neg)) {
                sb.append("      IRREDUCIBLE — same orbit. Your chain is not the problem: no orbit-constant "
                        + "invariant can ever separate orbit-mates (§10.4). Memory is forced; stop looking.\n");
                // The one escape, and it exists only here: a radial context has no orientation to preserve,
                // so an orientation-scoped axis (Position) may legally vary across the orbit (§3).
                if (key.radial())
                    sb.append("      …except: this context is RADIAL, which has no orientation to protect. An "
                            + "orientation-scoped axis (Position — legal on this track alone) can still split "
                            + "it. That is the one twin verdict with a way out.\n");
                continue;
            }
            final var base = CollisionDiagnostic.entropy(ls);
            final var best = CollisionDiagnostic.bestPrimitive(members, ls);
            if (best.gain() >= base - 1e-9) {
                sb.append(String.format("      SEPARABLE, battery has it — extend on '%s' (EXACT split, %.3f bits).%n",
                        best.primitive().name(), best.gain()));
                sb.append("        split: ").append(best.partition()).append('\n');
            } else if (best.gain() > 1e-9) {
                sb.append(String.format("      SEPARABLE, battery is PARTIAL — best is '%s' (%.3f of %.3f bits). "
                        + "Extending on it leaves a residual collision.%n", best.primitive().name(), best.gain(), base));
                sb.append("        split: ").append(best.partition()).append('\n');
            } else {
                sb.append("      SEPARABLE, battery is EMPTY — disjoint orbits, so a separating invariant "
                        + "exists, but nothing in the battery finds it. Over to you:\n");
                renderSides(sb, plexus, pos, neg);
            }
        }
        if (!any) sb.append("    none — the chain resolves every collision.\n");
        return sb.toString();
    }

    /** Show one representative per side, so the human has two shapes to look at rather than two integers. */
    private static void renderSides(StringBuilder sb, Plexus plexus, List<int[]> pos, List<int[]> neg) {
        sb.append("        positive:\n");
        Report.renderGrid(sb, "          ", pos.get(0), plexus.rows(pos.get(0)), plexus.cols(pos.get(0)), 'x', '.');
        sb.append("        negative:\n");
        Report.renderGrid(sb, "          ", neg.get(0), plexus.rows(neg.get(0)), plexus.cols(neg.get(0)), 'x', '.');
        sb.append("        (in a descriptor-only store these are regenerated with Fiber — legal invariants "
                + "are orbit-constant, so any representative evaluates identically to the original.)\n");
    }
}
