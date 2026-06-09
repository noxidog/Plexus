package org.tervel.plexus.diagnostics;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.Plexus.Key;
import org.tervel.plexus.ops.Fiber;
import org.tervel.plexus.symmetry.Shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The <b>combinatorial collision diagnostic</b> — automated invariant discovery. It runs as a training
 * post-pass: every context with <b>non-zero entropy</b> (both labels present — a collision the current
 * chain cannot resolve) is examined, and for each one that is <em>not</em> a provable symmetry-twin it
 * sweeps a battery of universal topological {@link Primitive}s, isolates the one that maximises
 * <b>information gain</b> across the colliding labels, and emits its {@link Primitive#spec() formal
 * specification} as the exact blueprint for the next manual {@code Invariant} extension.
 *
 * <h2>Twin vs. separable</h2>
 * Two grids are <b>provable symmetry-twins</b> when they lie in the same orbit — one is a rigid image of
 * the other (equal {@link Shape#of canonical pose}) — yet carry opposite labels. No group-invariant can
 * separate them (§10.4), so they are the irreducible floor and the diagnostic leaves them to the exact
 * exception store. A collision is <b>separable</b> exactly when the positive and negative label sets are
 * <em>disjoint as orbits</em> (no shared canonical shape): then some group-invariant differs across the
 * split, and the battery looks for which one.
 *
 * <h2>Fiber breadth</h2>
 * For a separable context it also runs {@link Fiber} to enumerate the full structural fiber — every grid
 * that routes there, seen or unseen — and reports over how many distinct values the winning primitive
 * spreads it, confirming the blueprint discriminates the whole fiber, not just the trained sample.
 *
 * <h2>Read as cognition: this is the expert</h2>
 * The diagnostic <em>is</em> the expert. A novice fails a collision by <b>underdetermination</b> — a scalar
 * with too many candidate carriers, no way to pick which difference accounts for it. The expert relocates the
 * failure all the way down to the <b>irreducible twin floor</b>, and names the separating axis when one
 * exists. Expertise does not remove the breakdown; it moves it from "underdetermined among many differences"
 * to "the two are genuine twins" (PLEXUS_DESIGN §11, <i>What the symmetry holds</i>).
 */
public final class CollisionDiagnostic implements Operation<String> {

    private final List<int[]> grids;
    private final List<Integer> labels;

    public CollisionDiagnostic(List<int[]> grids, List<Integer> labels) {
        this.grids = grids;
        this.labels = labels;
    }

    @Override
    public String apply(Plexus plexus) {
        final var side = grids.isEmpty() ? 0 : (int) Math.round(Math.sqrt(grids.get(0).length));

        // Group the labelled examples by the context they route to; an entropy>0 context is a collision.
        final var byKey = new LinkedHashMap<Key, List<int[]>>();
        final var labelByKey = new LinkedHashMap<Key, List<Integer>>();
        for (var i = 0; i < grids.size(); i++) {
            final var k = plexus.descriptor(grids.get(i));
            byKey.computeIfAbsent(k, x -> new ArrayList<>()).add(grids.get(i));
            labelByKey.computeIfAbsent(k, x -> new ArrayList<>()).add(labels.get(i));
        }

        final var sb = new StringBuilder("Collision diagnostic — automated invariant discovery\n");
        var collisions = 0;
        var twins = 0;
        var blueprints = 0;

        for (final var key : byKey.keySet()) {
            final var ls = labelByKey.get(key);
            if (ls.stream().distinct().count() < 2) continue;       // pure context — zero entropy, no collision
            collisions++;

            final var members = byKey.get(key);
            final var pos = new ArrayList<int[]>();
            final var neg = new ArrayList<int[]>();
            for (var i = 0; i < members.size(); i++) (ls.get(i) == 1 ? pos : neg).add(members.get(i));

            sb.append('\n').append(describe(plexus, key)).append('\n');
            sb.append(String.format("  collision: %d positive / %d negative (entropy %.3f bits)%n",
                    pos.size(), neg.size(), entropy(ls)));

            if (isProvableTwin(plexus, pos, neg)) {
                twins++;
                sb.append("  verdict: PROVABLE SYMMETRY-TWIN — irreducible by symmetry (§10.4); "
                        + "left to the exact exception store. No invariant can separate orbit-mates.\n");
                continue;
            }

            final var best = bestPrimitive(members, ls);
            blueprints++;
            sb.append(String.format("  verdict: SEPARABLE — colliding labels live in disjoint orbits.%n"));
            sb.append(String.format("  winning primitive: %s  (information gain %.3f / %.3f bits%s)%n",
                    best.primitive.name(), best.gain, entropy(ls),
                    best.gain >= entropy(ls) - 1e-9 ? ", EXACT split" : ""));
            sb.append("  blueprint (formal specification for the next Invariant):\n")
              .append("    ").append(best.primitive.spec()).append('\n');
            sb.append("    split: ").append(best.partition).append('\n');

            final var fiber = new Fiber(key, side).apply(plexus).toList();
            final var distinct = fiber.stream().map(best.primitive::quantized).distinct().count();
            sb.append(String.format("  fiber breadth: %d structures route here; primitive spreads them over "
                    + "%d distinct values.%n", fiber.size(), distinct));
        }

        if (collisions == 0) sb.append("\nNo entropy>0 contexts — the chain resolves every collision.\n");
        else sb.append(String.format("%nsummary: %d collision(s) — %d irreducible twin(s), %d separable "
                + "(blueprint emitted).%n", collisions, twins, blueprints));
        return sb.toString();
    }

    /**
     * The structured outcome of the diagnostic, for programmatic consumers (e.g. the renormalization
     * closure loop) that compare scales rather than read the human report. {@link #blueprint} is the SET of
     * topological-{@link Primitive} names emitted across the SEPARABLE collisions — the <b>reducible</b>
     * counterterms a richer chain could absorb; {@link #twinContexts} is the count of PROVABLE-TWIN
     * collisions — the <b>irreducible floor</b> that lands permanently in the exact-exception store.
     */
    public record Result(Set<String> blueprint, int separableContexts, int twinContexts, int collisions) { }

    /**
     * The structured counterpart to {@link #apply}: groups the labelled examples by context and, for every
     * entropy&gt;0 context, records whether it is a reducible SEPARABLE collision (its winning primitive's
     * name joins the blueprint) or an irreducible PROVABLE TWIN (the floor). Unlike {@code apply} it does
     * <em>not</em> enumerate fibers, so it is cheap to call repeatedly in a loop.
     */
    public Result analyze(Plexus plexus) {
        final var byKey = new LinkedHashMap<Key, List<int[]>>();
        final var labelByKey = new LinkedHashMap<Key, List<Integer>>();
        for (var i = 0; i < grids.size(); i++) {
            final var k = plexus.descriptor(grids.get(i));
            byKey.computeIfAbsent(k, x -> new ArrayList<>()).add(grids.get(i));
            labelByKey.computeIfAbsent(k, x -> new ArrayList<>()).add(labels.get(i));
        }
        final var blueprint = new LinkedHashSet<String>();
        var separable = 0;
        var twins = 0;
        var collisions = 0;
        for (final var key : byKey.keySet()) {
            final var ls = labelByKey.get(key);
            if (ls.stream().distinct().count() < 2) continue;          // pure context — no collision
            collisions++;
            final var members = byKey.get(key);
            final var pos = new ArrayList<int[]>();
            final var neg = new ArrayList<int[]>();
            for (var i = 0; i < members.size(); i++) (ls.get(i) == 1 ? pos : neg).add(members.get(i));
            if (isProvableTwin(plexus, pos, neg)) twins++;
            else { separable++; blueprint.add(bestPrimitive(members, ls).primitive().name()); }
        }
        return new Result(Collections.unmodifiableSet(blueprint), separable, twins, collisions);
    }

    // ---- twin test: do the positive and negative orbits intersect? --------------------------------

    /**
     * A collision is a provable twin iff some positive grid shares an orbit (canonical {@link Shape}) with
     * some negative grid — then that pair is a genuine symmetry-twin no invariant can split. Disjoint
     * orbits ⇒ separable.
     */
    private static boolean isProvableTwin(Plexus plexus, List<int[]> pos, List<int[]> neg) {
        final var group = plexus.group();
        final Set<Shape> posShapes = pos.stream().map(g -> Shape.of(g, group)).collect(Collectors.toSet());
        return neg.stream().map(g -> Shape.of(g, group)).anyMatch(posShapes::contains);
    }

    // ---- information-gain search over the primitive battery ---------------------------------------

    private record Scored(Primitive primitive, double gain, String partition) { }

    private static Scored bestPrimitive(List<int[]> members, List<Integer> ls) {
        final var base = entropy(ls);
        Scored best = null;
        for (final var p : Primitive.battery()) {
            // Partition the labelled members by this primitive's value; sum the per-bucket residual entropy.
            final var buckets = new LinkedHashMap<Long, List<Integer>>();
            for (var i = 0; i < members.size(); i++)
                buckets.computeIfAbsent(p.quantized(members.get(i)), x -> new ArrayList<>()).add(ls.get(i));
            var residual = 0.0;
            for (final var bucket : buckets.values())
                residual += (double) bucket.size() / members.size() * entropy(bucket);
            final var gain = base - residual;
            if (best == null || gain > best.gain) best = new Scored(p, gain, renderPartition(p, members, ls));
        }
        return best;
    }

    /** Render the winning split as {@code value→{pos:neg}} buckets, for the developer to eyeball. */
    private static String renderPartition(Primitive p, List<int[]> members, List<Integer> ls) {
        final var counts = new java.util.TreeMap<Double, int[]>();
        for (var i = 0; i < members.size(); i++) {
            final var v = p.valueOf(members.get(i));
            final var c = counts.computeIfAbsent(round(v), x -> new int[2]);
            c[ls.get(i)]++;
        }
        return counts.entrySet().stream()
                .map(e -> String.format("%s→{1:%d,0:%d}", trim(e.getKey()), e.getValue()[1], e.getValue()[0]))
                .collect(Collectors.joining("  "));
    }

    private static double round(double v) { return Math.round(v * 1_000_000.0) / 1_000_000.0; }
    private static String trim(double v)  { return v == Math.rint(v) ? Long.toString((long) v) : String.format("%.3f", v); }

    // ---- Shannon entropy in bits ------------------------------------------------------------------

    private static double entropy(List<Integer> ls) {
        if (ls.isEmpty()) return 0.0;
        final var ones = ls.stream().filter(l -> l == 1).count();
        final var p = (double) ones / ls.size();
        if (p == 0.0 || p == 1.0) return 0.0;
        return -(p * log2(p) + (1 - p) * log2(1 - p));
    }

    private static double log2(double x) { return Math.log(x) / Math.log(2); }

    private static String describe(Plexus plexus, Key key) {
        final var sym = key.radial() ? "(radial)" : "[" + plexus.group().describe(key.symmetry()) + "]";
        return "context " + sym + " " + plexus.coordString(key);
    }
}
