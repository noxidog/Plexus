package org.tervel.plexus;

import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.residual.ResidualResolver;
import org.tervel.plexus.symmetry.SymmetryGroup;
import org.tervel.plexus.trie.Trie;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * A <b>sector</b> — a symmetry class, and the unit at which the invariant chain may differ. Orbit-mates
     * have conjugate stabilizers and therefore the same canonical mask, so a sector is a union of whole
     * orbits: selecting a chain by sector can never route orbit-mates apart.
     *
     * <p>Radial is deliberately a <em>single</em> sector ({@code stabilizer == 0}). Subdividing it would
     * require putting the stabilizer into {@link RadialKey}, which drops it on purpose (a round shape has no
     * orientation, §4); without that column two radial sectors running different chains could produce equal
     * coordinate lists and silently merge. The oriented track carries its stabilizer in the key, so it
     * subdivides safely.
     */
    public record Sector(boolean radial, int stabilizer) {
        /** The sector a key belongs to — every key determines one, which is what makes the chain recoverable. */
        public static Sector of(Key k) { return new Sector(k.radial(), k.symmetry()); }
    }

    /**
     * Which coordinates a sector routes on. The chain is no longer forced to be uniform: by orbit–stabilizer
     * {@code |orbit| = |G| / |Stab|}, so a coordinate — which must be constant on an orbit to be legal
     * (§3) — has to agree on <em>fewer</em> grids the more symmetry the sector has. The legal vocabulary
     * therefore <b>grows monotonically up the subgroup lattice</b>, reaching everything at full symmetry,
     * where the orbit is a single grid and any function of it is trivially orbit-constant.
     *
     * <p>The shipped {@link #uniform()} is exactly the historical behaviour — one declared chain, gated only
     * by the radial verdict — so this generalises the radial/oriented split (a two-level special case) into
     * a ladder with one rung per conjugacy class of stabilizer, without changing any existing result.
     */
    @FunctionalInterface
    public interface ChainPolicy {

        /** The coordinates {@code sector} routes on, drawn from the {@code declared} chain. */
        List<Invariant> chainFor(Sector sector, List<Invariant> declared);

        /** The historical policy: one chain for everything, gated only by {@link Invariant#applies}. */
        static ChainPolicy uniform() {
            return (sector, declared) -> declared.stream().filter(d -> d.applies(sector.radial())).toList();
        }

        /**
         * <b>Withhold</b> coordinates from low-symmetry sectors: the {@code withheld} coordinates route only
         * in oriented sectors whose orbit is at most {@code maxOrbit}; everywhere else the chain is the
         * declared vocabulary minus them. This is the theorem above made operational — a coordinate too
         * pose-dependent to be legal in the trivial sector (orbit 8 under D4) can be perfectly legal where
         * the orbit is 1 or 2, because there are that many fewer grids for it to agree across.
         *
         * <p>Withholding rather than appending matters twice over: a coordinate the caller already declared
         * would otherwise appear <em>twice</em> in the rich sectors, and rebuilding the chain by filtering
         * the declared list preserves its <b>order</b> — which is load-bearing, since chain order is the
         * trie's rung order (§5). Pass the same {@link Invariant} instances that appear in the declared
         * chain; membership is by equality.
         *
         * <p>Radial is exempt, being a single sector ({@link Sector}).
         */
        static ChainPolicy byOrbit(SymmetryGroup group, int maxOrbit, List<Invariant> withheld) {
            final var order = group.transforms().size();
            return (sector, declared) -> {
                final var applicable = declared.stream().filter(d -> d.applies(sector.radial()));
                final var fixers = Integer.bitCount(sector.stabilizer());
                final var rich = !sector.radial() && fixers > 0 && order / fixers <= maxOrbit;
                return rich ? applicable.toList() : applicable.filter(d -> !withheld.contains(d)).toList();
            };
        }
    }

    /**
     * One routing context (bucket): its {@code [neg, pos]} counts and its exact exceptions. It owns its
     * answer.
     *
     * <p>This is a <b>derived view</b> over a leaf of the store, materialised on demand — not the storage
     * itself, and not mutable. In particular {@code exceptions} is computed rather than sealed: a member is
     * an exception exactly when its own recorded majority contradicts its leaf's, both read from live
     * counts. That removes the two-pass training the frozen list used to require, and it fixes a defect the
     * frozen list had — a grid seen under <em>both</em> labels used to be sealed as an exception once per
     * contradicting example, which flipped it away from its own majority (a grid 3:2 in favour of 1 answered
     * 0). Comparing majorities answers such a grid with the label it mostly carries.
     */
    public record Context(long[] counts, List<int[]> exceptions) {

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
     * The domain frame. {@code rows, cols > 0} pin an explicit rectangular domain (non-square — paired with a
     * D2 {@link SymmetryGroup}); {@code <= 0} means "infer a square frame per grid" ({@code sqrt(length)}),
     * the historical default. The frame feeds the geometry of {@link #descriptor} (the canonical stabilizer
     * and the radial test); it does not change the square path.
     */
    private final int rows;
    private final int cols;

    /**
     * The intra-context decision policy for the part structure cannot resolve — the residual past the twin
     * floor (§10.4). Routing is exact and deterministic up to the context; this resolver owns the leaf
     * decision inside it. It is injected (a peer of the group and chain): the deterministic
     * {@link ExactExceptions} is what ships, but a brute-force or learned resolver can take its place.
     */
    private final ResidualResolver resolver;

    /** Which of the declared coordinates each symmetry class routes on ({@link ChainPolicy}). */
    private final ChainPolicy policy;

    /**
     * The store. Keys are held as <b>paths</b> — the track, then the stabilizer edge, then one edge per
     * chain coordinate — rather than as whole tuples in a hash map, so every prefix of a key is a live node
     * and not a thing recomputed by regrouping. {@link Context} is a <em>view</em> over a leaf, derived from
     * live counts on demand; it is not stored, which is why there is no seal pass here (an exception is a
     * member whose own majority contradicts its leaf's, and both sides move together).
     *
     * <p>The trie is constructed with an <b>empty extension vocabulary</b>, so its paths are exactly the
     * descriptor's keys and {@link #contexts} is a faithful enumeration. Handing it a vocabulary turns on
     * local self-extension (§5), which deliberately makes paths longer than the declared key — a different
     * contract, and not the one the router's public {@code Key} surface promises.
     */
    private final Trie store = new Trie(this);

    /**
     * Construct a router from its three injected parts: the symmetry {@code group}, the {@code invariants}
     * chain, and the {@code resolver} for the residual past the twin floor. All three are the caller's
     * choice — the resolver is a peer of the group and chain, not a hidden default — so the deterministic
     * {@link ExactExceptions} is passed in explicitly rather than assumed here.
     */
    public Plexus(SymmetryGroup group, List<Invariant> invariants, ResidualResolver resolver) {
        this(group, invariants, resolver, -1, -1);
    }

    /**
     * Construct a router over an explicit {@code rows×cols} domain. For a square domain pass the
     * three-argument constructor (or {@code rows == cols}); for a rectangular domain pass the two distinct
     * dimensions and a D2 group ({@link org.tervel.plexus.symmetry.Dihedral#d2()}).
     */
    public Plexus(SymmetryGroup group, List<Invariant> invariants, ResidualResolver resolver, int rows, int cols) {
        this(group, invariants, resolver, rows, cols, ChainPolicy.uniform());
    }

    /**
     * Construct a router whose chain may differ <b>per sector</b>. The {@code invariants} list becomes the
     * <em>declared</em> vocabulary and {@code policy} decides which of it each symmetry class actually routes
     * on; {@link ChainPolicy#uniform()} reproduces the single-chain behaviour of the other constructors.
     */
    public Plexus(SymmetryGroup group, List<Invariant> invariants, ResidualResolver resolver,
                  int rows, int cols, ChainPolicy policy) {
        this.group = group;
        this.invariants = List.copyOf(invariants);
        this.resolver = resolver;
        this.rows = rows;
        this.cols = cols;
        this.policy = policy;
    }

    /** Row count of {@code g}'s frame — the pinned domain height, or the inferred square side. */
    public int rows(int[] g) { return rows > 0 ? rows : (int) Math.round(Math.sqrt(g.length)); }

    /** Column count of {@code g}'s frame — the pinned domain width (row stride), or the inferred square side. */
    public int cols(int[] g) { return cols > 0 ? cols : (int) Math.round(Math.sqrt(g.length)); }

    // ---- Routing: resolve a shape to its context key ---------------------------------------

    /**
     * Resolves a shape to its key: the symmetry verdict (radial, or the rotation-canonical stabilizer)
     * plus the coordinates of every invariant that {@link Invariant#applies} to that verdict.
     */
    public Key descriptor(int[] g) {
        final var sector = sector(g);
        final var coords = chainFor(sector).stream().map(d -> d.applyAsInt(g)).toList();
        return sector.radial() ? new RadialKey(coords) : new OrientedKey(sector.stabilizer(), coords);
    }

    /** The symmetry class {@code g} routes in — the radial verdict, plus the canonical stabilizer if oriented. */
    public Sector sector(int[] g) {
        final var radial = isRadial(g);
        return new Sector(radial, radial ? 0 : group.canonicalStabilizer(g, rows(g), cols(g)));
    }

    /** The coordinates a sector routes on — the declared chain as filtered by the injected {@link ChainPolicy}. */
    public List<Invariant> chainFor(Sector sector) { return policy.chainFor(sector, invariants); }

    /** The coordinates behind a key. Every key determines its sector, so its chain is always recoverable. */
    public List<Invariant> chainFor(Key key) { return chainFor(Sector.of(key)); }

    /** The context a grid resolves to, as a view over its leaf; null if the full path was never trained. */
    private Context contextFor(int[] grid) {
        final var leaf = store.leafFor(grid);
        return leaf == null ? null : new Context(leaf.counts(), leaf.dissenters());
    }

    /** Verdict: a round shape (some rotation fixes its centred form) has no orientation. Public because it
     *  is the cheapest prefix of {@link #descriptor} — a per-edge (lazy) router needs the track verdict
     *  without paying for the stabilizer and the chain. */
    public boolean isRadial(int[] g) {
        final var b = Invariant.box(g, rows(g), cols(g));
        if (b[0] != b[1]) return false;                 // non-square box ⇒ rot90 transposes it ⇒ can't be radial
        final var c = centered(g, rows(g), cols(g));
        return c != null && group.rotationFixes(c, rows(g), cols(g));
    }

    /** Translates the blob so its bounding box is centered on a {@code rows×cols} grid; null if empty. */
    private static int[] centered(int[] g, int rows, int cols) {
        int minR = rows, maxR = -1, minC = cols, maxC = -1;
        for (var i = 0; i < g.length; i++) if (g[i] == 1) {
            final int r = i / cols, c = i % cols;
            minR = Math.min(minR, r); maxR = Math.max(maxR, r);
            minC = Math.min(minC, c); maxC = Math.max(maxC, c);
        }
        if (maxR < 0) return null;
        final var offR = (rows - 1 - (maxR - minR)) / 2 - minR;
        final var offC = (cols - 1 - (maxC - minC)) / 2 - minC;
        final var out = new int[g.length];
        for (var i = 0; i < g.length; i++) if (g[i] == 1) {
            final int r = i / cols + offR, c = i % cols + offC;
            if (r >= 0 && r < rows && c >= 0 && c < cols) out[r * cols + c] = 1;
        }
        return out;
    }

    /** Public centering: translate the blob so its bounding box sits in the middle of a square grid (null if empty). */
    public static int[] center(int[] g) {
        final var s = (int) Math.round(Math.sqrt(g.length));
        return centered(g, s, s);
    }

    /** The applicable discriminator coordinates of a key, by name (e.g. {@code {sig:167}}, or {@code {sig:167, deg:2}}). */
    public java.util.LinkedHashMap<String, Integer> namedCoords(Key k) {
        final var names = chainFor(k).stream().map(Invariant::name).toList();
        final var out = new java.util.LinkedHashMap<String, Integer>();
        for (var i = 0; i < k.coords().size() && i < names.size(); i++) out.put(names.get(i), k.coords().get(i));
        return out;
    }

    // ---- Training / scoring ----------------------------------------------------------------

    /** One online insert: walk the path, bump every rung. Legal at any time — there is nothing to reseal. */
    @Override
    public void accept(int[] input, Integer label) {
        store.add(input, label == 1 ? 1 : 0);
    }

    /**
     * The three <b>distinct</b> outcomes of a resolution. {@link #score} collapses the last two onto
     * {@code 0}, which is lossy in a way that matters: "the structure routed you to a context whose answer
     * is negative" and "no example ever routed here" are different epistemic states — one is a judgement,
     * the other is the honest admission that routing has nothing to say. A caller that cannot tell them
     * apart will read silence as denial (the {@link org.tervel.plexus.ops.Searcher} did exactly that,
     * counting never-explored transforms among a grid's "surviving" symmetries).
     */
    public enum Verdict {
        /** Routed to a context, and the residual resolver answered positive. */
        POSITIVE,
        /** Routed to a context, and the residual resolver answered negative. */
        NEGATIVE,
        /** No context — this key was never trained. Not a negative: an absence of evidence. */
        UNSEEN
    }

    /**
     * Resolve a grid to its {@link Verdict} — the full-information form of {@link #score}. Same single
     * lookup; it simply declines to spend the reject signal.
     */
    public Verdict verdict(int[] grid) {
        final var c = contextFor(grid);
        if (c == null) return Verdict.UNSEEN;                      // unseen key → nothing to say
        return resolver.resolve(c, grid) == 1 ? Verdict.POSITIVE : Verdict.NEGATIVE;
    }

    /**
     * The binary answer: 1 for a positive resolution, 0 for both a negative one and an unseen key. Retained
     * as the scoring surface (a confusion matrix wants a bit), but prefer {@link #verdict} anywhere the
     * difference between "judged negative" and "never seen" can change what the caller concludes.
     */
    public int score(int[] inputs) {
        return verdict(inputs) == Verdict.POSITIVE ? 1 : 0;
    }

    /**
     * Trains on a labelled dataset — the batch form of {@link #accept}, and public for the same reason
     * {@code accept} is: training is part of the router's surface, not an accident of package layout.
     *
     * <p>It runs in <b>one</b> pass. The historical second pass existed only to seal each
     * context's exceptions against a converged majority — a contradiction could not be identified early,
     * because the majority it contradicts was not yet known. That constraint belonged to the storage, not to
     * the problem: a leaf that keeps its members derives the same contradictions from live counts at read
     * time, so a later insert that flips a majority flips them with it. Nothing is frozen, so nothing can go
     * stale, and {@link #accept} is legal at any point afterwards.
     */
    public void fit(List<int[]> grids, List<Integer> labels) {
        for (var i = 0; i < grids.size(); i++) accept(grids.get(i), labels.get(i));
        resolver.seal(this, grids, labels);   // structure has converged; let the residual policy train itself
    }

    // ---- Routing accessors (the context decides; these just expose the routing) -------------

    /**
     * Every context, as a read-only view for inspection and reporting — the trie's leaves, each rebuilt into
     * the {@link Key} whose path reaches it and the {@link Context} its counts imply. Oriented first, then
     * radial. This is a <b>projection</b>, materialised per call, not the storage itself: the store keeps
     * paths, and a whole key is only reassembled when a report asks to iterate them.
     */
    public Map<Key, Context> contexts() {
        final var merged = new java.util.LinkedHashMap<Key, Context>();
        store.forEachLeaf((radial, stabilizer, coords, counts, dissenters) -> {
            final var values = coords.stream().map(Long::intValue).toList();
            final Key key = radial ? new RadialKey(values) : new OrientedKey((int) stabilizer, values);
            merged.put(key, new Context(counts, dissenters));
        });
        return java.util.Collections.unmodifiableMap(merged);
    }

    /** The context a grid resolves to (null if its key was never seen). */
    public Context context(int[] grid) { return contextFor(grid); }

    /** A named rendering of a key's discriminator coordinates, e.g. "sig=167" (or "sig=167 deg=2"). */
    public String coordString(Key k) {
        final var names = chainFor(k).stream().map(Invariant::name).toList();
        final var parts = new ArrayList<String>();
        for (var i = 0; i < k.coords().size() && i < names.size(); i++)
            parts.add(names.get(i) + "=" + k.coords().get(i));
        return String.join(" ", parts);
    }

    /**
     * The store itself. Exposed because the storage has a shape worth inspecting that {@link #contexts}
     * cannot show: contexts are the <em>leaves</em>, while the store also holds every prefix, and it is the
     * prefixes that reveal where a coordinate stopped deciding anything (§5, the census).
     */
    public Trie store() { return store; }

    /** The symmetry group this Plexus routes by — the set of transforms operations sweep over. */
    public SymmetryGroup group() { return group; }

    /**
     * The <b>declared</b> invariant vocabulary — every coordinate the caller injected. What any given sector
     * actually routes on is {@link #chainFor}, which may be a subset; under {@link ChainPolicy#uniform()}
     * the two coincide up to the radial gate.
     */
    public List<Invariant> invariants() { return invariants; }
}
