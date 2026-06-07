package org.tervel.plexus;

import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.residual.ResidualResolver;
import org.tervel.plexus.symmetry.SymmetryGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A symmetry-keyed classifier. Every input is <b>resolved</b> to a {@link Context} by pure structural
 * routing — a symmetry verdict (rotation-canonical self-symmetry, or "radial" for round shapes) refined
 * by an injected chain of orthogonal, symmetry-invariant {@link Invariant}s (signature, and the opt-in
 * max-degree / position) into a composite {@link Key}, one of two sealed tracks. A context owns its own
 * decision: a majority label, with the leaf decision past the twin floor delegated to an injected
 * {@link org.tervel.plexus.residual.ResidualResolver} (by default an exact set of memorised exceptions —
 * the irreducible symmetry-twin contradictions). The Plexus only routes; the context decides.
 */
public class Plexus implements BiConsumer<int[], Integer> {

    /**
     * A resolved routing key. The symmetry verdict (radial vs oriented) is no longer a column packed
     * <em>inside</em> one flat tuple — it is the <b>type</b> of the key. The root router branches on it
     * once, into two strongly-typed tracks ({@link RadialKey}, {@link OrientedKey}), each carrying only the
     * coordinates that apply to it. This keeps every coordinate table uniform within its track (no
     * ragged radial-only column hanging off an oriented key) and turns the old conditional-array routing
     * into compile-time polymorphic dispatch (a {@code switch} over the sealed permits).
     *
     * <p>The three accessors are kept for the report/operation surface, which reads keys uniformly:
     * {@link #radial()} is the branch tag, {@link #symmetry()} the oriented stabilizer (0 on the radial
     * branch, which has no orientation), {@link #coords()} the applicable invariant coordinates.
     */
    public sealed interface Key permits RadialKey, OrientedKey {
        boolean radial();
        int symmetry();
        List<Integer> coords();
    }

    /** The radial track: round shapes with no orientation. Keyed on position-sensitive coordinates only. */
    public record RadialKey(List<Integer> coords) implements Key {
        @Override public boolean radial()  { return true; }
        @Override public int     symmetry() { return 0; }     // no orientation ⇒ no stabilizer column
    }

    /** The oriented track: shapes with a meaningful orientation. Keyed on the canonical stabilizer + coords. */
    public record OrientedKey(int symmetry, List<Integer> coords) implements Key {
        @Override public boolean radial() { return false; }
    }

    /** One routing context (bucket): its [neg, pos] counts and its exact exceptions. It owns its answer. */
    public record Context(long[] counts, List<int[]> exceptions) {
        static Context empty() { return new Context(new long[2], new ArrayList<>()); }
        void bump(int label)       { counts[label]++; }
        void addException(int[] g) { exceptions.add(g.clone()); }

        /** The context's answer: its majority label. */
        public int answer() { return counts[1] > counts[0] ? 1 : 0; }
        /** Whether this context's majority is the positive class. */
        public boolean isTarget() { return answer() == 1; }
        /** Whether {@code g} is a memorised exception of this context. */
        public boolean isException(int[] g) { return exceptions.stream().anyMatch(e -> Arrays.equals(e, g)); }
        public int  exceptionCount() { return exceptions.size(); }
        public long pos() { return counts[1]; }
        public long neg() { return counts[0]; }
    }

    private final SymmetryGroup group;
    private final List<Invariant> invariants;

    /**
     * The intra-context decision policy for the part structure cannot resolve — the residual past the twin
     * floor (§9.4). Routing is exact and deterministic up to the context; this resolver owns the leaf
     * decision inside it. It is injected (a peer of the group and chain): the deterministic
     * {@link ExactExceptions} is what ships, but a brute-force or learned resolver can take its place.
     */
    private final ResidualResolver resolver;

    /**
     * The two decoupled context tables — one per track. Splitting the single map into a {@link RadialKey}-
     * keyed and an {@link OrientedKey}-keyed table is the storage half of the branching: each table holds
     * only keys of its own track, so a lookup never mixes the two coordinate layouts. {@link #contextFor}
     * dispatches a key to its table by sealed-type {@code switch}.
     */
    private final Map<RadialKey, Context> radialContexts = new HashMap<>();
    private final Map<OrientedKey, Context> orientedContexts = new HashMap<>();

    /**
     * Construct a router from its three injected parts: the symmetry {@code group}, the {@code invariants}
     * chain, and the {@code resolver} for the residual past the twin floor. All three are the caller's
     * choice — the resolver is a peer of the group and chain, not a hidden default — so the deterministic
     * {@link ExactExceptions} is passed in explicitly rather than assumed here.
     */
    public Plexus(SymmetryGroup group, List<Invariant> invariants, ResidualResolver resolver) {
        this.group = group;
        this.invariants = List.copyOf(invariants);
        this.resolver = resolver;
    }

    // ---- Routing: resolve a shape to its context key ---------------------------------------

    /**
     * Resolves a shape to its key: the symmetry verdict (radial, or the rotation-canonical stabilizer)
     * plus the coordinates of every invariant that {@link Invariant#applies} to that verdict.
     */
    public Key descriptor(int[] g) {
        final var radial = isRadial(g);
        final var coords = invariants.stream().filter(d -> d.applies(radial)).map(d -> d.applyAsInt(g)).toList();
        return radial ? new RadialKey(coords) : new OrientedKey(group.canonicalStabilizer(g), coords);
    }

    /** Dispatch a key to its track's context table; {@code create} computes an empty context if absent. */
    private Context contextFor(Key key, boolean create) {
        return switch (key) {
            case RadialKey r -> create ? radialContexts.computeIfAbsent(r, k -> Context.empty()) : radialContexts.get(r);
            case OrientedKey o -> create ? orientedContexts.computeIfAbsent(o, k -> Context.empty()) : orientedContexts.get(o);
        };
    }

    /** Verdict: a round shape (some rotation fixes its centred form) has no orientation. */
    private boolean isRadial(int[] g) {
        final var b = Invariant.box(g);
        if (b[0] != b[1]) return false;                 // non-square box ⇒ rot90 transposes it ⇒ can't be radial
        final var c = centered(g, (int) Math.round(Math.sqrt(g.length)));
        return c != null && group.rotationFixes(c);
    }

    /** Translates the blob so its bounding box is centered on the grid; null if the grid is empty. */
    private static int[] centered(int[] g, int side) {
        int minR = side, maxR = -1, minC = side, maxC = -1;
        for (var i = 0; i < g.length; i++) if (g[i] == 1) {
            final int r = i / side, c = i % side;
            minR = Math.min(minR, r); maxR = Math.max(maxR, r);
            minC = Math.min(minC, c); maxC = Math.max(maxC, c);
        }
        if (maxR < 0) return null;
        final var offR = (side - 1 - (maxR - minR)) / 2 - minR;
        final var offC = (side - 1 - (maxC - minC)) / 2 - minC;
        final var out = new int[g.length];
        for (var i = 0; i < g.length; i++) if (g[i] == 1) {
            final int r = i / side + offR, c = i % side + offC;
            if (r >= 0 && r < side && c >= 0 && c < side) out[r * side + c] = 1;
        }
        return out;
    }

    /** Public centering: translate the blob so its bounding box sits in the middle of the grid (null if empty). */
    public static int[] center(int[] g) {
        return centered(g, (int) Math.round(Math.sqrt(g.length)));
    }

    /** The applicable discriminator coordinates of a key, by name (e.g. {@code {sig:167}}, or {@code {sig:167, deg:2}}). */
    public java.util.LinkedHashMap<String, Integer> namedCoords(Key k) {
        final var names = invariants.stream().filter(d -> d.applies(k.radial())).map(Invariant::name).toList();
        final var out = new java.util.LinkedHashMap<String, Integer>();
        for (var i = 0; i < k.coords().size() && i < names.size(); i++) out.put(names.get(i), k.coords().get(i));
        return out;
    }

    // ---- Training / scoring ----------------------------------------------------------------

    @Override
    public void accept(int[] input, Integer label) {
        contextFor(descriptor(input), true).bump(label == 1 ? 1 : 0);
    }

    public int score(int[] inputs) {
        final var c = contextFor(descriptor(inputs), false);
        if (c == null) return 0;                                   // unseen key → reject
        return resolver.resolve(c, inputs);                        // structure routed; residual decides the leaf
    }

    /**
     * Trains on a labelled dataset in two internal passes: first count every example into its context,
     * then seal each context's exact exceptions — grids whose label contradicts their context's now-
     * converged majority. The seal pass must follow the count pass (a context's majority is only known
     * once all counts are in), and it must re-read the same data, which is why training takes the whole
     * dataset rather than streaming: the contradictions can only be identified against the final counts.
     */
    void fit(List<int[]> grids, List<Integer> labels) {
        for (var i = 0; i < grids.size(); i++) accept(grids.get(i), labels.get(i));
        for (var i = 0; i < grids.size(); i++) {
            final var g = grids.get(i);
            final var c = contextFor(descriptor(g), false);
            if (c != null && labels.get(i) != c.answer()) c.addException(g);
        }
        resolver.seal(this, grids, labels);   // structure has converged; let the residual policy train itself
    }

    // ---- Routing accessors (the context decides; these just expose the routing) -------------

    /**
     * The contexts of both tracks merged into one read-only view for inspection and reporting — radial
     * keys first, then oriented. The two tables stay decoupled in storage; this is only the union the
     * whole-table operations (the {@link org.tervel.plexus.ops.Topology} walk) iterate over.
     */
    public Map<Key, Context> contexts() {
        final var merged = new java.util.LinkedHashMap<Key, Context>(radialContexts.size() + orientedContexts.size());
        merged.putAll(radialContexts);
        merged.putAll(orientedContexts);
        return java.util.Collections.unmodifiableMap(merged);
    }

    /** The context a grid resolves to (null if its key was never seen). */
    public Context context(int[] grid) { return contextFor(descriptor(grid), false); }

    /** A named rendering of a key's discriminator coordinates, e.g. "sig=167" (or "sig=167 deg=2"). */
    public String coordString(Key k) {
        final var names = invariants.stream().filter(d -> d.applies(k.radial())).map(Invariant::name).toList();
        final var parts = new ArrayList<String>();
        for (var i = 0; i < k.coords().size() && i < names.size(); i++)
            parts.add(names.get(i) + "=" + k.coords().get(i));
        return String.join(" ", parts);
    }

    /** The symmetry group this Plexus routes by — the set of transforms operations sweep over. */
    public SymmetryGroup group() { return group; }

    /** The injected invariant chain — the coordinate maps whose components the Jacobian volume differentiates. */
    public List<Invariant> invariants() { return invariants; }
}
