package org.tervel.plexus.trie;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.diagnostics.Primitive;
import org.tervel.plexus.symmetry.Shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * The routing table stored as <b>paths</b> rather than whole keys, so every prefix is a live context. One
 * structure with two knobs; the configurations that motivated it are settings, not subclasses:
 *
 * <ul>
 *   <li><b>{@code vocabulary}</b> — the {@link Primitive} columns a colliding leaf may grow a new edge on.
 *       <b>Empty</b> means the chain is fixed and the trie only ever stores what the {@link Plexus} routes
 *       by; non-empty makes it <b>self-extending</b> under an {@link ExtensionPolicy}.</li>
 *   <li><b>{@link LeafMemory}</b> — {@code GRIDS} keeps each member verbatim; {@code DESCRIPTORS} keeps only
 *       its measured columns plus its canonical orbit, so identical descriptors merge into one row.</li>
 * </ul>
 *
 * <h2>Self-describing</h2>
 * Each node carries the {@link Axis} its children are keyed by rather than indexing a global chain by depth.
 * That is what lets extension be <b>local</b> — one node deepens, nothing else moves — and depth-indexing
 * falls out as the special case where the axis list never grows. Above the leaves the axes are structural
 * (the stabilizer edge on the oriented track, then the sector's chain, §4); below them they are discovered.
 *
 * <h2>Training is the walk</h2>
 * One example = walk the path, bump {@code [neg, pos]} at every node passed. The write walk necessarily
 * reaches the exact leaf, and it touches every rung a read will ever consult, so all prefix counts and
 * purity verdicts stay fresh by construction. A leaf derives a member's answer from live counts rather than
 * freezing an exception list, so <b>no seal pass exists to go stale</b> and inserts are legal at any time.
 *
 * <h2>Reading is the same walk, lazily</h2>
 * Coordinates are evaluated only when their edge is needed, and the descent <b>stops at the first pure
 * node</b>: one-sided counts mean no deeper coordinate can change the answer. A pure node holds no
 * contradiction, so the early answer equals the full walk's on every seen key. The price is the reject
 * signal — a pure-stop cannot tell a seen key from an unseen one that agrees up to the stop, which is why
 * {@link #leafFor} (the backing-store path) deliberately does not stop early.
 *
 * <p>Formatting lives in {@code reports/}, information theory in {@code diagnostics/}, and the collision
 * decision in {@link ExtensionPolicy}; what remains here is the structure and the two walks over it.
 */
public final class Trie {

    /** What a leaf remembers about each member. */
    public enum LeafMemory { GRIDS, DESCRIPTORS }

    private static final class Node {
        final Map<Long, Node> children = new HashMap<>();
        final long[] counts = new long[2];
        final int depth;
        Axis splitOn;                               // null ⇒ leaf
        Map<Member, long[]> members;                // leaf only
        boolean irreducible;                        // set when the orbit test forbade a split

        Node(int depth) { this.depth = depth; }
        int  answer()  { return counts[1] > counts[0] ? 1 : 0; }
        boolean pure() { return counts[0] == 0 || counts[1] == 0; }
        long total()   { return counts[0] + counts[1]; }
    }

    private final Plexus plexus;
    private final List<Primitive> vocabulary;
    private final LeafMemory memory;
    private final ExtensionPolicy extension;
    private final Node radialRoot = new Node(0);
    private final Node orientedRoot = new Node(0);
    private int splits;

    /** A fixed-chain trie keeping members verbatim — the configuration that backs the router. */
    public Trie(Plexus plexus) { this(plexus, List.of(), LeafMemory.GRIDS, ExtensionPolicy.never()); }

    /** A trie over an explicit extension {@code vocabulary} (empty = fixed chain), memory, and policy. */
    public Trie(Plexus plexus, List<Primitive> vocabulary, LeafMemory memory, ExtensionPolicy extension) {
        this.plexus = plexus;
        this.vocabulary = List.copyOf(vocabulary);
        this.memory = memory;
        this.extension = extension;
    }

    /** The self-extending configuration: the standard battery, descriptors, and information-gain splits. */
    public static Trie growing(Plexus plexus) {
        return new Trie(plexus, Primitive.battery(), LeafMemory.DESCRIPTORS, ExtensionPolicy.informationGain());
    }

    // ---- the structural prefix ----------------------------------------------------------------

    /** The axes above any discovered one: the stabilizer edge (oriented), then the sector's chain. */
    private List<Axis> structural(int[] grid, boolean radial) {
        final var out = new ArrayList<Axis>();
        if (!radial) out.add(Axis.structural("stab",
                g -> plexus.group().canonicalStabilizer(g, plexus.rows(g), plexus.cols(g))));
        for (final var inv : plexus.chainFor(plexus.sector(grid)))
            out.add(Axis.structural(inv.name(), inv::applyAsInt));
        return out;
    }

    /** The declared depth bound — exact for a fixed chain, an upper bound once extension may deepen a path. */
    private int bound(boolean radial) {
        return (radial ? 0 : 1)
                + (int) plexus.invariants().stream().filter(d -> d.applies(radial)).count();
    }

    private Member member(int[] grid) {
        // Only the twin test reads the orbit, and only extension runs it — so with no vocabulary the
        // square-frame Shape.of is never called, which is what keeps the D2 domain working.
        final var orbit = vocabulary.isEmpty() ? null : Shape.of(grid, plexus.group());
        if (memory == LeafMemory.GRIDS) return new Member.Grid(grid.clone(), orbit);
        final var axes = new long[vocabulary.size()];
        for (var i = 0; i < axes.length; i++) axes[i] = vocabulary.get(i).quantized(grid);
        return new Member.Descriptor(axes, orbit);
    }

    // ---- write ---------------------------------------------------------------------------------

    /** One online insert: walk the path bumping every rung, record the member, then try to extend. */
    public void add(int[] grid, int label) {
        final var l = label == 1 ? 1 : 0;
        final var radial = plexus.isRadial(grid);
        final var structural = structural(grid, radial);
        var node = radial ? radialRoot : orientedRoot;
        node.counts[l]++;

        while (node.splitOn != null || node.depth < structural.size()) {
            if (node.splitOn == null) node.splitOn = structural.get(node.depth);
            final var edge = node.splitOn.onGrid().applyAsLong(grid);
            final var parent = node;
            node = node.children.computeIfAbsent(edge, e -> new Node(parent.depth + 1));
            node.counts[l]++;
        }

        if (node.members == null) node.members = new LinkedHashMap<>();
        node.members.computeIfAbsent(member(grid), m -> new long[2])[l]++;
        extend(node);
    }

    /** Batch training is the online insert in a loop — one pass, no seal. */
    public void fit(List<int[]> grids, List<Integer> labels) {
        for (var i = 0; i < grids.size(); i++) add(grids.get(i), labels.get(i));
    }

    /**
     * The collision decision, taken where the collision happens. The <b>orbit test comes first and is not
     * negotiable</b>: members sharing one orbit are provable symmetry-twins (§10.4), so no coordinate could
     * separate them and the leaf is flagged irreducible without consulting anyone. Only past that is the
     * {@link ExtensionPolicy} asked for a column; if it names one, the leaf grows that edge and redistributes
     * its members by <em>reading</em> the column, which is why a store that discarded its grids can still
     * split. Each child is then offered the same choice, so one insert may deepen a leaf by several rungs.
     *
     * <p>Termination is structural: redistribution makes each child constant in the chosen column, so that
     * column scores zero gain below and is never chosen twice on a path.
     */
    private void extend(Node leaf) {
        if (vocabulary.isEmpty() || leaf.pure() || leaf.members == null) return;
        if (oneOrbit(leaf)) { leaf.irreducible = true; return; }

        final var col = extension.chooseColumn(leaf.counts, leaf.members, vocabulary);
        if (col < 0) return;                               // the policy declined: nothing separates them

        leaf.splitOn = new Axis(vocabulary.get(col).name(), g -> vocabulary.get(col).quantized(g), col);
        splits++;
        for (final var e : leaf.members.entrySet()) {
            final var child = leaf.children.computeIfAbsent(
                    e.getKey().column(col, vocabulary), k -> new Node(leaf.depth + 1));
            child.counts[0] += e.getValue()[0];
            child.counts[1] += e.getValue()[1];
            if (child.members == null) child.members = new LinkedHashMap<>();
            child.members.put(e.getKey(), e.getValue().clone());
        }
        leaf.members = null;
        for (final var child : leaf.children.values()) extend(child);
    }

    /** Whether every member of an impure leaf shares one orbit — the provable-twin condition. */
    private static boolean oneOrbit(Node leaf) {
        return leaf.members.keySet().stream().map(Member::orbit).distinct().count() == 1;
    }

    // ---- read ----------------------------------------------------------------------------------

    /**
     * Route a grid down its own path, evaluating each coordinate only when its edge is needed and stopping
     * at the first pure node. At the leaf a seen member answers with its own recorded majority — exact for
     * everything the stored form distinguishes; only an irreducible leaf falls back to the bare majority.
     */
    public Resolution resolve(int[] grid) {
        final var radial = plexus.isRadial(grid);
        var node = radial ? radialRoot : orientedRoot;
        final var bound = bound(radial);
        if (node.total() == 0) return new Resolution(0, -1, bound, -1, 0, 0, false, 0, false, false, false);
        if (node.pure())
            return new Resolution(node.answer(), 0, bound, 0, node.counts[0], node.counts[1],
                    false, 0, false, false, false);

        // The stabilizer edge names the sector, so the sector's chain is known exactly when the walk first
        // needs an invariant — laziness survives a per-sector ChainPolicy (§4).
        var structural = radial ? structural(grid, true) : List.<Axis>of();
        var fullDepth = radial ? structural.size() : bound;
        var depth = 0;
        var evals = 0;
        var pureAt = -1;
        var backoff = false;

        while (node.splitOn != null || depth < fullDepth) {
            if (node.splitOn == null) {
                if (depth >= structural.size()) break;
                node.splitOn = structural.get(depth);
            }
            final var edge = node.splitOn.onGrid().applyAsLong(grid);
            evals++;
            if (!radial && depth == 0) {                   // sector now known; the bound becomes the truth
                structural = structural(grid, false);
                fullDepth = structural.size();
            }
            final var next = node.children.get(edge);
            if (next == null) { backoff = true; break; }
            node = next;
            depth++;
            if (pureAt < 0 && node.pure()) pureAt = depth;
            if (node.pure()) break;                        // pure-stop: nothing deeper can change the answer
        }

        final var own = node.members == null ? null : node.members.get(member(grid));
        final var label = own == null ? node.answer() : (own[1] > own[0] ? 1 : 0);
        return new Resolution(label, depth, fullDepth, pureAt, node.counts[0], node.counts[1],
                own != null && label != node.answer(), evals, backoff, own != null, node.irreducible);
    }

    // ---- backing-store surface -----------------------------------------------------------------

    /** One leaf, handed out as the path that reaches it plus what it holds. */
    public interface LeafConsumer {
        /**
         * @param radial      which track
         * @param stabilizer  the first edge on the oriented track ({@code 0} on the radial one)
         * @param coords      the remaining edges, in chain order
         * @param counts      the leaf's {@code [neg, pos]}
         * @param dissenters  members whose own majority contradicts the leaf's — derived from live counts,
         *                    never a frozen list, so a later insert that flips the majority flips these too
         */
        void accept(boolean radial, long stabilizer, List<Long> coords, long[] counts, List<int[]> dissenters);
    }

    /**
     * Walk every leaf — radial track first, then oriented, and children in ascending edge order at every
     * node. This is what lets the trie serve the flat {@code Map<Key, Context>} view, and the ordering is
     * <b>deterministic by construction</b> rather than whatever a hash table happened to yield, so reports
     * built on it are reproducible run to run.
     */
    public void forEachLeaf(LeafConsumer consumer) {
        walk(radialRoot, true, new ArrayList<>(), consumer);
        walk(orientedRoot, false, new ArrayList<>(), consumer);
    }

    private void walk(Node node, boolean radial, List<Long> path, LeafConsumer consumer) {
        if (node.total() == 0) return;
        if (node.splitOn == null) {
            final var offset = radial || path.isEmpty() ? 0 : 1;
            final var stabilizer = offset == 0 ? 0L : path.get(0);
            consumer.accept(radial, stabilizer, List.copyOf(path.subList(offset, path.size())),
                    node.counts.clone(), dissenters(node));
            return;
        }
        for (final var edge : node.children.keySet().stream().sorted().toList()) {
            path.add(edge);
            walk(node.children.get(edge), radial, path, consumer);
            path.remove(path.size() - 1);
        }
    }

    /** Members of a leaf whose own recorded majority contradicts the leaf's — the exceptions, derived. */
    private static List<int[]> dissenters(Node node) {
        final var out = new ArrayList<int[]>();
        if (node.members == null) return out;
        final var majority = node.answer();
        for (final var e : node.members.entrySet())
            if (e.getKey() instanceof Member.Grid g && (e.getValue()[1] > e.getValue()[0] ? 1 : 0) != majority)
                out.add(g.cells());
        return out;
    }

    /**
     * Route to a leaf <b>without</b> pure-stopping, so the seen/unseen distinction survives. {@link #resolve}
     * trades that signal for skipped evaluations, which is right for the lazy read and wrong for a backing
     * store: a pure-stop cannot tell a trained key from an untrained one that agrees up to the stop, and
     * {@link Plexus#verdict} exists precisely to keep them apart.
     *
     * @return the leaf's {@code [neg, pos]} and dissenters, or null if the full path was never trained
     */
    public Leaf leafFor(int[] grid) {
        final var radial = plexus.isRadial(grid);
        var node = radial ? radialRoot : orientedRoot;
        if (node.total() == 0) return null;
        while (node.splitOn != null) {
            final var next = node.children.get(node.splitOn.onGrid().applyAsLong(grid));
            if (next == null) return null;                 // unseen: the path runs out before a leaf
            node = next;
        }
        return new Leaf(node.counts.clone(), dissenters(node));
    }

    /** A leaf's contents, independent of how it was reached. */
    public record Leaf(long[] counts, List<int[]> dissenters) { }

    // ---- structural inspection (what the reports read) ------------------------------------------

    /**
     * One node, flattened for inspection. Reports read this instead of the node itself, so formatting can
     * live in {@code reports/} without the structure leaking its internals.
     */
    public record NodeView(int depth, boolean radial, String edge, long neg, long pos,
                           boolean pure, boolean parentPure, boolean irreducible,
                           String splitOn, int rows) {
        public boolean leaf() { return splitOn == null; }
        public long total()   { return neg + pos; }
    }

    /** Pre-order visit of every node, in the same deterministic order as {@link #forEachLeaf}. */
    public void forEachNode(java.util.function.Consumer<NodeView> visitor) {
        visit(radialRoot, true, "root", false, visitor);
        visit(orientedRoot, false, "root", false, visitor);
    }

    private void visit(Node node, boolean radial, String edge, boolean parentPure,
                       java.util.function.Consumer<NodeView> visitor) {
        if (node.total() == 0) return;
        visitor.accept(new NodeView(node.depth, radial, edge, node.counts[0], node.counts[1],
                node.pure(), parentPure, node.irreducible,
                node.splitOn == null ? null : node.splitOn.name(),
                node.members == null ? 0 : node.members.size()));
        if (node.splitOn == null) return;
        for (final var key : node.children.keySet().stream().sorted().toList())
            visit(node.children.get(key), radial, node.splitOn.name() + "=" + key, node.pure(), visitor);
    }

    /** The rung names of a track, for a report that wants to label depths: root, stabilizer, then the chain. */
    public List<String> rungs(boolean radial) {
        final var names = new ArrayList<String>();
        names.add(radial ? "(all radial)" : "(all oriented)");
        if (!radial) names.add("stab");
        plexus.invariants().stream().filter(d -> d.applies(radial)).map(i -> i.name()).forEach(names::add);
        return names;
    }

    // ---- tallies --------------------------------------------------------------------------------

    /** How many local extensions the data forced. */
    public int splits() { return splits; }

    /** Leaves holding a single orbit under both labels — the contradictions no column could split. */
    public int irreducibleLeaves() { return tally(n -> n.irreducible ? 1 : 0); }

    /** Rows stored across every leaf: the structure's entire memory. */
    public int storedRows() { return tally(n -> n.members == null ? 0 : n.members.size()); }

    private int tally(ToIntFunction<Node> f) {
        return tally(orientedRoot, f) + tally(radialRoot, f);
    }

    private int tally(Node node, ToIntFunction<Node> f) {
        if (node.splitOn == null) return f.applyAsInt(node);
        var n = 0;
        for (final var child : node.children.values()) n += tally(child, f);
        return n;
    }
}
