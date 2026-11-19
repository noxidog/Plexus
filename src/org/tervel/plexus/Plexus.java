package org.tervel.plexus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plexus: a symmetry-routed exact classifier that files every shape by what it is, not where it sits.
 * Two tries, one per lattice:
 *
 * <pre>
 * D4 root / D6 root → box → symmetry walk → mass → joins → [extensions] → orientation → pose → corner
 * </pre>
 *
 * <ul>
 *   <li>The <b>box</b> (short × long, or the hex extent) and the <b>lattice</b> come from the bounding box of
 *       the non-zero pixels ({@link Descriptor}).</li>
 *   <li>The <b>symmetry walk</b> asks one question per symmetry, yes or no, in the lattice's fixed order. Every
 *       partial symmetry is therefore a node, and a slice over symmetries is a walk.</li>
 *   <li><b>Mass</b> and <b>joins</b>: filled cells, and adjacent pairs on the shape's own lattice.</li>
 *   <li><b>Extensions</b> are shape coordinates self-extension inserts when different shapes collide.</li>
 *   <li>The <b>trailing</b> levels (orientation, pose, corner) are last, so a read only needs them where labels
 *       disagree across poses or positions.</li>
 * </ul>
 *
 * <p>Training walks the whole path, adding one to every node passed, and keeps the grid at the leaf. Reading
 * walks until the path breaks off; a pure node may then stand in for the missing child if the {@link Extrapolate}
 * setting allows that level (an {@link Verdict#ASSUMED} answer). Slices walk the same tries under constraints and
 * stream every matching grid, trained or assumed, to a callback.
 *
 * <p>All grids share one input frame ({@code inRows × inCols}), row-major. It is how a grid's cells are read
 * into rows and columns, and where slices place the grids they generate.
 */
public final class Plexus {

    // ---- the trie --------------------------------------------------------------------------------

    /** What a node's children split on: a level, plus the walk index or the extension primitive. */
    record Coordinate(Level level, int index, Primitive primitive) {
        static final Coordinate BOX = new Coordinate(Level.BOX, 0, null);
        static Coordinate symmetry(int i) { return new Coordinate(Level.SYMMETRY, i, null); }
        static final Coordinate MASS = new Coordinate(Level.MASS, 0, null);
        static final Coordinate JOINS = new Coordinate(Level.JOINS, 0, null);
        static Coordinate extension(Primitive p) { return new Coordinate(Level.EXTENSION, 0, p); }
        static final Coordinate ORIENTATION = new Coordinate(Level.ORIENTATION, 0, null);
        static final Coordinate POSE = new Coordinate(Level.POSE, 0, null);
        static final Coordinate POSITION = new Coordinate(Level.POSITION, 0, null);
    }

    /** One trained grid, kept at its leaf so exact exceptions and subtree rebuilds have the evidence. */
    record Member(int[] grid, int label, Descriptor descriptor) { }

    static final class Node {
        Coordinate splitOn;                                   // null at a leaf
        final Map<Long, Node> children = new LinkedHashMap<>();
        final long[] counts = new long[2];
        final List<Member> members = new ArrayList<>();      // leaves only
        final Node parent;
        final Coordinate edge;                                // the parent's splitOn (null at a root)
        final long value;                                     // the edge value
        final Kind kind;                                      // null at a root
        final int depth;

        Node(Coordinate splitOn, Node parent, Coordinate edge, long value, Kind kind) {
            this.splitOn = splitOn; this.parent = parent; this.edge = edge; this.value = value; this.kind = kind;
            this.depth = parent == null ? 0 : parent.depth + 1;
        }

        boolean pure() { return counts[0] == 0 || counts[1] == 0; }
        int answer() { return counts[1] > counts[0] ? 1 : 0; }
    }

    private final int inRows;
    private final int inCols;
    private final boolean selfExtend;
    private final Set<Lattice> lattices;
    private final Node d4 = new Node(Coordinate.BOX, null, null, 0, null);
    private final Node d6 = new Node(Coordinate.BOX, null, null, 0, null);

    /** A router over {@code inRows × inCols} input grids: the default lattices, self-extension on. */
    public Plexus(int inRows, int inCols) { this(inRows, inCols, Lattice.DEFAULT, true); }

    /**
     * A router over {@code inRows × inCols} input grids on these lattices, self-extension on. For example
     * {@code new Plexus(8, 8, Lattice.SQUARE_8)} keeps every shape square with 8 neighbours, and
     * {@code new Plexus(8, 8, Lattice.SQUARE_8, Lattice.HEX_6)} also tries hex.
     *
     * @throws IllegalArgumentException if the lattices are not a legal set ({@link Lattice#checked(Set)})
     */
    public Plexus(int inRows, int inCols, Lattice... lattices) { this(inRows, inCols, Lattice.checked(lattices), true); }

    /**
     * A router over {@code inRows × inCols} input grids on {@code lattices}, with self-extension on or off.
     * Both are fixed for the router's life: they define the key, so every grid it ever describes must be
     * described the same way.
     *
     * @throws IllegalArgumentException if the lattices are not a legal set ({@link Lattice#checked(Set)})
     */
    public Plexus(int inRows, int inCols, Set<Lattice> lattices, boolean selfExtend) {
        this.inRows = inRows; this.inCols = inCols;
        this.lattices = Lattice.checked(lattices);
        this.selfExtend = selfExtend;
    }

    /** The lattices this router was built on. */
    public Set<Lattice> lattices() { return lattices; }

    /** Whether self-extension runs after training. */
    public boolean selfExtends() { return selfExtend; }

    public int inRows() { return inRows; }
    public int inCols() { return inCols; }

    /** The descriptor this router reads off {@code grid}. */
    public Descriptor describe(int[] grid) { return Descriptor.of(grid, inRows, inCols, lattices); }

    private Node rootOf(Root r) { return r == Root.D6 ? d6 : d4; }

    // ---- training -----------------------------------------------------------------------------------

    /** Train on one grid, then self-extend (if on). Legal at any time. */
    public void accept(int[] grid, int label) {
        insert(grid, label);
        if (selfExtend) extend();
    }

    /** Train on a labelled dataset in one pass, then self-extend once. */
    public void fit(List<int[]> grids, List<Integer> labels) {
        for (var i = 0; i < grids.size(); i++) insert(grids.get(i), labels.get(i));
        if (selfExtend) extend();
    }

    private void insert(int[] grid, int label) {
        final var d = describe(grid);
        final var root = rootOf(d.root());
        root.counts[label == 1 ? 1 : 0]++;
        descend(root, new Member(grid.clone(), label == 1 ? 1 : 0, d));
    }

    /** Walk {@code m} down from {@code node} (whose counts already include it), creating nodes as needed. */
    private static void descend(Node node, Member m) {
        while (node.splitOn != null) {
            final var v = value(node.splitOn, m.descriptor());
            final var kind = node.kind == null ? m.descriptor().kind() : node.kind;
            final var from = node;
            node = node.children.computeIfAbsent(v, k -> new Node(next(from.splitOn, kind), from, from.splitOn, k, kind));
            node.counts[m.label()]++;
        }
        node.members.add(m);
    }

    /** The coordinate a node splits on after {@code c}, in the default order for {@code kind}. */
    static Coordinate next(Coordinate c, Kind kind) {
        final var walk = Groups.walkFor(kind).size();
        return switch (c.level()) {
            case BOX -> kind == Kind.EMPTY ? null : walk > 0 ? Coordinate.symmetry(0) : Coordinate.MASS;
            case SYMMETRY -> c.index() + 1 < walk ? Coordinate.symmetry(c.index() + 1) : Coordinate.MASS;
            case MASS -> Coordinate.JOINS;
            case JOINS, EXTENSION -> kind == Kind.SQUARE ? Coordinate.POSE : Coordinate.ORIENTATION;
            case ORIENTATION -> Coordinate.POSE;
            case POSE -> Coordinate.POSITION;
            case POSITION -> null;
        };
    }

    static long value(Coordinate c, Descriptor d) {
        return switch (c.level()) {
            case BOX -> d.kind() == Kind.EMPTY ? 0 : d.boxShort() * 4096L + d.boxLong();
            case SYMMETRY -> d.answers()[c.index()] ? 1 : 0;
            case MASS -> d.mass();
            case JOINS -> d.joins();
            case EXTENSION -> c.primitive().value(d.canonical(), d.lattice());
            case ORIENTATION -> d.orientation();
            case POSE -> d.pose();
            case POSITION -> d.cornerRow() * 4096L + d.cornerCol();
        };
    }

    // ---- self-extension -----------------------------------------------------------------------------

    /**
     * Grow shape coordinates where different shapes collide. A junction (the node just above the trailing
     * levels) that holds both labels is checked: if its members are all one shape, the trailing levels already
     * separate them and nothing is added. If they are several shapes, the battery primitive with the best
     * information gain (and not already on the path) is inserted there, and the subtree is rebuilt from the
     * stored members. Repeats until no junction changes.
     */
    public void extend() {
        for (var changed = true; changed; ) {
            changed = false;
            for (final var root : List.of(d4, d6))
                for (final var node : junctions(root))
                    if (extendAt(node)) changed = true;
        }
    }

    private static List<Node> junctions(Node root) {
        final var out = new ArrayList<Node>();
        collect(root, out);
        return out;
    }

    private static void collect(Node n, List<Node> out) {
        if (n.splitOn != null && n.kind != null && n.splitOn.equals(next(Coordinate.JOINS, n.kind))) out.add(n);
        for (final var c : n.children.values()) collect(c, out);
    }

    private boolean extendAt(Node node) {
        if (node.pure()) return false;
        final var members = membersUnder(node);
        if (members.stream().map(m -> m.descriptor().canonical()).distinct().count() < 2) return false;
        final var used = new HashSet<String>();
        for (var n = node; n != null; n = n.parent)
            if (n.splitOn != null && n.splitOn.level() == Level.EXTENSION) used.add(n.splitOn.primitive().name());
        Primitive best = null;
        var bestGain = 1e-12;
        for (final var p : Primitive.BATTERY) {
            if (used.contains(p.name())) continue;
            final var parts = new HashMap<Integer, long[]>();
            for (final var m : members)
                parts.computeIfAbsent(p.value(m.descriptor().canonical(), m.descriptor().lattice()), k -> new long[2])[m.label()]++;
            final var gain = Information.gain(node.counts, parts.values());
            if (gain > bestGain) { bestGain = gain; best = p; }
        }
        if (best == null) return false;
        node.splitOn = Coordinate.extension(best);
        node.children.clear();
        for (final var m : members) descend(node, m);
        return true;
    }

    /**
     * The mixed nodes that matter, and what resolves each. At a junction (the node just above the trailing
     * levels): one shape split by orientation, pose or corner; several shapes separated by an inserted
     * coordinate; or several shapes no primitive separates. At a leaf: the same grid trained with both labels,
     * or different shapes that share one full key. Other mixed nodes (above a junction, or between it and the
     * leaves) are not listed.
     */
    public String diagnose() {
        final var sb = new StringBuilder("mixed nodes and what resolves them\n");
        var found = 0;
        for (final var root : List.of(d4, d6)) {
            final var nodes = new ArrayList<Node>();
            collectAll(root, nodes);
            for (final var n : nodes) {
                if (n.pure() || n.parent == null) continue;
                final String verdict;
                if (n.splitOn != null && n.splitOn.level() == Level.EXTENSION)
                    verdict = "several shapes: separated by " + n.splitOn.primitive().name();
                else if (n.splitOn != null && n.splitOn.equals(next(Coordinate.JOINS, n.kind))) {
                    final var shapes = membersUnder(n).stream().map(m -> m.descriptor().canonical()).distinct().count();
                    verdict = shapes == 1 ? "one shape: separated by orientation, pose or corner"
                            : "several shapes: no primitive separates them; kept as members at the leaves";
                } else if (n.splitOn == null) {
                    final var grids = n.members.stream().map(m -> Arrays.toString(m.grid())).distinct().count();
                    verdict = grids == 1 ? "the same grid trained with both labels"
                            : "different shapes with one full key: each stored grid answers with its own label";
                } else continue;
                found++;
                sb.append("  ").append(context(n)).append('\n').append("    -> ").append(verdict).append('\n');
            }
        }
        if (found == 0) sb.append("  none: every node is pure\n");
        return sb.toString();
    }

    private static void collectAll(Node n, List<Node> out) {
        out.add(n);
        for (final var c : n.children.values()) collectAll(c, out);
    }

    private static List<Member> membersUnder(Node n) {
        final var out = new ArrayList<Member>();
        gather(n, out);
        return out;
    }

    private static void gather(Node n, List<Member> out) {
        out.addAll(n.members);
        for (final var c : n.children.values()) gather(c, out);
    }

    // ---- reading ------------------------------------------------------------------------------------

    /** Read {@code grid} under the default setting ({@link Extrapolate#SYMMETRY}). */
    public Answer read(int[] grid) { return read(grid, Extrapolate.SYMMETRY); }

    /** Read {@code grid}: follow its path; where it breaks off, a pure node may stand in if the extrapolation setting allows. */
    public Answer read(int[] grid, Extrapolate extrapolate) {
        final var d = describe(grid);
        var node = rootOf(d.root());
        while (node.splitOn != null) {
            final var child = node.children.get(value(node.splitOn, d));
            if (child == null) {
                if (standsIn(node, d, extrapolate)) return new Answer(Verdict.ASSUMED, node.answer(), context(node));
                return new Answer(Verdict.UNSEEN, -1, context(node));
            }
            node = child;
        }
        final var label = resolve(node, grid);
        return new Answer(label == 1 ? Verdict.POSITIVE : Verdict.NEGATIVE, label, context(node));
    }

    /**
     * Whether pure {@code node} may answer for {@code d}, whose child there is missing. The setting must allow the
     * level. Under {@link Extrapolate#SYMMETRY} a stand-in for an unseen symmetry class also needs the
     * query's mass and joins to occur together somewhere under the node, so the fold never crosses mass or joins.
     */
    private static boolean standsIn(Node node, Descriptor d, Extrapolate extrapolate) {
        if (node.parent == null || !node.pure() || node.counts[0] + node.counts[1] == 0) return false;
        if (!extrapolate.allows(node.splitOn.level())) return false;
        return node.splitOn.level() != Level.SYMMETRY || extrapolate == Extrapolate.EVERYTHING || invariantsUnder(node, d.mass(), d.joins());
    }

    private static boolean invariantsUnder(Node n, int mass, int joins) {
        if (n.edge != null && n.edge.level() == Level.JOINS && n.value == joins
                && n.parent.edge != null && n.parent.edge.level() == Level.MASS && n.parent.value == mass) return true;
        for (final var c : n.children.values()) if (invariantsUnder(c, mass, joins)) return true;
        return false;
    }

    /** The binary score: 1 for POSITIVE or an ASSUMED 1 under the default setting, else 0. */
    public int score(int[] grid) {
        final var a = read(grid);
        return (a.verdict() == Verdict.POSITIVE || (a.verdict() == Verdict.ASSUMED && a.label() == 1)) ? 1 : 0;
    }

    /** At a leaf: a grid seen there answers with its own majority; any other grid with the leaf's. */
    private static int resolve(Node leaf, int[] grid) {
        final var own = new long[2];
        for (final var m : leaf.members) if (Arrays.equals(m.grid(), grid)) own[m.label()]++;
        if (own[0] + own[1] == 0 || own[0] == own[1]) return leaf.answer();
        return own[1] > own[0] ? 1 : 0;
    }

    private Context context(Node node) {
        final var steps = new ArrayList<Context.Step>();
        var root = node;
        for (var n = node; n.parent != null; n = n.parent) { steps.add(0, step(n)); root = n.parent; }
        if (node.parent == null) root = node;
        final var boundary = node.parent != null && !node.parent.pure();
        return new Context(root == d6 ? Root.D6 : Root.D4, List.copyOf(steps), node.counts[0], node.counts[1],
                node.answer(), node.depth, boundary);
    }

    private static Context.Step step(Node n) {
        final var c = n.edge;
        final var v = n.value;
        return switch (c.level()) {
            case BOX -> new Context.Step(Level.BOX, "box", v, n.kind == Kind.EMPTY ? "empty"
                    : n.kind == Kind.HEX ? "hex " + (v % 4096) : (v / 4096) + "x" + (v % 4096));
            case SYMMETRY -> new Context.Step(Level.SYMMETRY, Groups.walkFor(n.kind).get(c.index()).name(), v, v == 1 ? "yes" : "no");
            case MASS -> new Context.Step(Level.MASS, "mass", v, Long.toString(v));
            case JOINS -> new Context.Step(Level.JOINS, "joins", v, Long.toString(v));
            case EXTENSION -> new Context.Step(Level.EXTENSION, c.primitive().name(), v, Long.toString(v));
            case ORIENTATION -> new Context.Step(Level.ORIENTATION, n.kind == Kind.HEX ? "shift" : "turned", v,
                    n.kind == Kind.HEX ? (v == 0 ? "right" : "left") : (v == 1 ? "yes" : "no"));
            case POSE -> new Context.Step(Level.POSE, "pose", v, Long.toString(v));
            case POSITION -> new Context.Step(Level.POSITION, "corner", v, "(" + (v / 4096) + "," + (v % 4096) + ")");
        };
    }

    // ---- slices and generation ----------------------------------------------------------------------

    /** Where a grid's path ends: the node it reached, and whether it broke off there (a missing child). */
    private record Stop(Node node, boolean missing) { }

    private Stop stop(int[] grid) {
        final var d = describe(grid);
        var node = rootOf(d.root());
        while (node.splitOn != null) {
            final var child = node.children.get(value(node.splitOn, d));
            if (child == null) return new Stop(node, true);
            node = child;
        }
        return new Stop(node, false);
    }

    private record Pending(int[] grid, Context context) { }

    /** Largest number of whole-orbit fills (or subsets) a single node may enumerate. */
    static final long GENERATION_LIMIT = 1L << 21;

    /**
     * Walk the slice's root(s) under its constraints. Trained grids are delivered as they are found
     * (CONFIRMED); grids assumed by a pure node within the setting are generated, then delivered deepest node
     * first and, at equal depth, nodes next to disagreement first (ASSUMED).
     *
     * <p>With {@link Slice.Per#POSE} or {@link Slice.Per#SHAPE} the matching grids are folded into one entry
     * per distinct pose or shape. The entry is a trained grid if the group has one (CONFIRMED), and among
     * those the one nearest the canonical pose and the frame's top-left: not turned, lowest pose code, lowest
     * corner. So a shape trained in its canonical pose is shown in it.
     */
    public Slice.Result slice(Slice q, Slice.Callback callback) {
        if (q.per == Slice.Per.PLACEMENT) return placements(q, callback);

        // Folding by pose or by shape: gather every matching grid, then keep one per group.
        record Found(int[] grid, Context context, Slice.Evidence evidence, Descriptor d) { }
        final var found = new ArrayList<Found>();
        final var all = placements(q, (g, c, e) -> found.add(new Found(g, c, e, describe(g))));
        final Comparator<Found> preferred = Comparator
                .comparing((Found f) -> f.evidence() != Slice.Evidence.CONFIRMED)   // a trained grid first
                .thenComparingInt(f -> Math.max(0, f.d().orientation()))
                .thenComparingInt(f -> f.d().pose())
                .thenComparingInt(f -> f.d().cornerRow())
                .thenComparingInt(f -> f.d().cornerCol());
        final var groups = new LinkedHashMap<List<Object>, Found>();
        for (final var f : found) {
            final List<Object> key = q.per == Slice.Per.SHAPE
                    ? List.of(f.d().root(), f.d().canonical())
                    : List.of(f.d().root(), f.d().canonical(), f.d().orientation(), f.d().pose());
            groups.merge(key, f, (a, b) -> preferred.compare(a, b) <= 0 ? a : b);
        }
        var confirmed = 0;
        for (final var f : groups.values()) {
            if (f.evidence() == Slice.Evidence.CONFIRMED) confirmed++;
            callback.accept(f.grid(), f.context(), f.evidence());
        }
        return new Slice.Result(confirmed, groups.size() - confirmed, all.skipped());
    }

    /** Every matching grid, one entry per placement. */
    private Slice.Result placements(Slice q, Slice.Callback callback) {
        final var roots = q.root == Root.BOTH ? List.of(d4, d6) : List.of(rootOf(q.root));
        final var seen = new HashSet<String>();
        final var pending = new ArrayList<Pending>();
        final var tally = new int[3];                          // confirmed, assumed, skipped
        for (final var root : roots) walk(root, q, callback, seen, pending, tally);
        pending.sort(Comparator.comparingInt((Pending p) -> -p.context().depth())
                .thenComparing(p -> !p.context().boundary()));
        for (final var p : pending) callback.accept(p.grid(), p.context(), Slice.Evidence.ASSUMED);
        return new Slice.Result(tally[0], pending.size(), tally[2]);
    }

    private void walk(Node node, Slice q, Slice.Callback cb, Set<String> seen, List<Pending> pending, int[] tally) {
        if (node.splitOn == null) {
            final var ctx = context(node);
            final var done = new HashSet<String>();
            for (final var m : node.members) {
                final var key = Arrays.toString(m.grid());
                if (!done.add(key)) continue;
                final var label = resolve(node, m.grid());
                if (q.label != null && q.label != label) continue;
                seen.add(key);
                tally[0]++;
                cb.accept(m.grid().clone(), ctx, Slice.Evidence.CONFIRMED);
            }
            return;
        }
        for (final var e : node.children.entrySet())
            if (passes(node, e.getKey(), e.getValue(), q)) walk(e.getValue(), q, cb, seen, pending, tally);
        if (node.parent != null && node.pure() && node.counts[0] + node.counts[1] > 0
                && q.extrapolate.allows(node.splitOn.level()) && (q.label == null || q.label == node.answer()))
            generate(node, q, seen, pending, tally);
    }

    private static boolean passes(Node node, long v, Node child, Slice q) {
        return switch (node.splitOn.level()) {
            case BOX -> {
                if (q.boxShort != null && (v / 4096 != q.boxShort || v % 4096 != q.boxLong)) yield false;
                if (child.kind == Kind.EMPTY) yield q.required.isEmpty() && q.massMin == 0;
                final var names = Groups.walkFor(child.kind).stream().map(CellSet.Map::name).toList();
                yield names.containsAll(q.required);
            }
            case SYMMETRY -> {
                final var name = Groups.walkFor(node.kind).get(node.splitOn.index()).name();
                if (q.required.contains(name)) yield v == 1;
                yield !q.exact || v == 0;
            }
            case MASS -> v >= q.massMin && v <= q.massMax;
            case JOINS -> v >= q.joinsMin && v <= q.joinsMax;
            case ORIENTATION -> q.orientation == null || v == q.orientation;
            case POSE -> q.pose == null || v == q.pose;
            case POSITION -> q.cornerRow == null || v == q.cornerRow * 4096L + q.cornerCol;
            case EXTENSION -> true;
        };
    }

    /** Generate the grids a pure node would answer for but never saw, keeping those the slice admits. */
    private void generate(Node node, Slice q, Set<String> seen, List<Pending> pending, int[] tally) {
        final var level = node.splitOn.level();
        final Set<CellSet> crops = switch (level) {
            case ORIENTATION, POSE, POSITION -> memberCrops(node);
            default -> shapes(node, q, tally);
        };
        final var ctx = context(node);
        for (final var crop : crops)
            for (final var t : Groups.D4) {
                final var posed = crop.map(t);
                for (var r = 0; r + posed.extentA <= inRows; r++)
                    for (var c = 0; c + posed.extentB <= inCols; c++) {
                        final var g = place(posed, r, c);
                        final var key = Arrays.toString(g);
                        if (seen.contains(key)) continue;
                        final var s = stop(g);
                        if (s.node() != node || !s.missing()) continue;
                        final var d = describe(g);
                        if (!standsIn(node, d, q.extrapolate) || !q.admits(d)) continue;
                        seen.add(key);
                        pending.add(new Pending(g, ctx));
                    }
            }
    }

    /** The input-frame crop of one member per distinct shape under {@code node}. */
    private Set<CellSet> memberCrops(Node node) {
        final var reps = new LinkedHashMap<CellSet, Member>();
        for (final var m : membersUnder(node)) reps.putIfAbsent(m.descriptor().canonical(), m);
        final var out = new LinkedHashSet<CellSet>();
        for (final var m : reps.values()) out.add(cropOf(m.grid()));
        return out;
    }

    /**
     * Shapes in this node's box with at least the symmetries already answered yes on the path. On the square
     * lattice they are built from whole orbits of those symmetries (one choice per orbit); on the hex root the
     * candidates are the subsets of the square input box, which the descriptor then files.
     */
    private Set<CellSet> shapes(Node node, Slice q, int[] tally) {
        Node box = node;
        while (box.edge == null || box.edge.level() != Level.BOX) box = box.parent;
        final var kind = box.kind;
        if (kind == Kind.EMPTY) return Set.of();
        final int h = (int) (box.value / 4096), w = (int) (box.value % 4096);
        final var yes = new ArrayList<Integer>();
        for (var n = node; n != box.parent; n = n.parent)
            if (n.edge != null && n.edge.level() == Level.SYMMETRY && n.value == 1) yes.add(n.edge.index());

        if (kind == Kind.HEX) return hexCandidates(h, q, tally);

        final var maps = new ArrayList<int[]>();                      // each: a permutation of the h×w cells
        for (final var i : yes) maps.add(boxPermutation(kind, i, h, w));
        final var orbit = orbits(h * w, maps);
        final var groups = Arrays.stream(orbit).distinct().count();
        if ((1L << Math.min(groups, 62)) > GENERATION_LIMIT) { tally[2]++; return Set.of(); }
        final var members = new HashMap<Integer, List<Integer>>();
        for (var i = 0; i < orbit.length; i++) members.computeIfAbsent(orbit[i], k -> new ArrayList<>()).add(i);
        final var list = new ArrayList<>(members.values());
        final var out = new LinkedHashSet<CellSet>();
        for (long mask = 1; mask < (1L << list.size()); mask++) {
            final var fill = new int[h * w];
            var mass = 0;
            for (var k = 0; k < list.size(); k++)
                if ((mask & (1L << k)) != 0) for (final var cell : list.get(k)) { fill[cell] = 1; mass++; }
            if (mass < q.massMin || mass > q.massMax || !tight(fill, h, w)) continue;
            out.add(cropOfFill(fill, w));
        }
        return out;
    }

    private Set<CellSet> hexCandidates(int s, Slice q, int[] tally) {
        final var cells = s * s;
        if (cells >= 62 || (1L << cells) > GENERATION_LIMIT) { tally[2]++; return Set.of(); }
        final var out = new LinkedHashSet<CellSet>();
        for (long mask = 1; mask < (1L << cells); mask++) {
            final var mass = Long.bitCount(mask);
            if (mass < q.massMin || mass > q.massMax) continue;
            final var fill = new int[cells];
            for (var i = 0; i < cells; i++) if ((mask & (1L << i)) != 0) fill[i] = 1;
            if (!tight(fill, s, s)) continue;
            out.add(cropOfFill(fill, s));
        }
        return out;
    }

    private static CellSet cropOfFill(int[] fill, int w) {
        final var as = new ArrayList<Integer>();
        final var bs = new ArrayList<Integer>();
        for (var i = 0; i < fill.length; i++) if (fill[i] == 1) { as.add(i / w); bs.add(i % w); }
        return CellSet.of(as.stream().mapToInt(Integer::intValue).toArray(), bs.stream().mapToInt(Integer::intValue).toArray());
    }

    /** The permutation of an {@code h×w} box's cells (row-major) that walk symmetry {@code i} performs. */
    private static int[] boxPermutation(Kind kind, int i, int h, int w) {
        final var p = new int[h * w];
        for (var a = 0; a < h; a++)
            for (var b = 0; b < w; b++) {
                final int[] to = switch (kind) {
                    case RECT -> switch (i) {
                        case 0 -> new int[]{h - 1 - a, w - 1 - b};      // rot180
                        case 1 -> new int[]{a, w - 1 - b};              // short-axis mirror
                        default -> new int[]{h - 1 - a, b};             // long-axis mirror
                    };
                    default -> switch (i) {                             // square box, s = h = w
                        case 0 -> new int[]{b, h - 1 - a};              // rot90
                        case 1 -> new int[]{h - 1 - a, h - 1 - b};      // rot180
                        case 2 -> new int[]{h - 1 - a, b};              // mirrorH
                        case 3 -> new int[]{a, h - 1 - b};              // mirrorV
                        case 4 -> new int[]{b, a};                      // diagMain
                        default -> new int[]{h - 1 - b, h - 1 - a};     // diagAnti
                    };
                };
                p[a * w + b] = to[0] * w + to[1];
            }
        return p;
    }

    /** Orbit label per cell under the group generated by {@code maps} (union-find). */
    private static int[] orbits(int n, List<int[]> maps) {
        final var parent = new int[n];
        for (var i = 0; i < n; i++) parent[i] = i;
        for (final var m : maps)
            for (var i = 0; i < n; i++) {
                final int a = find(parent, i), b = find(parent, m[i]);
                if (a != b) parent[a] = b;
            }
        final var out = new int[n];
        for (var i = 0; i < n; i++) out[i] = find(parent, i);
        return out;
    }

    private static int find(int[] p, int i) { while (p[i] != i) { p[i] = p[p[i]]; i = p[i]; } return i; }

    private static boolean tight(int[] fill, int h, int w) {
        boolean top = false, bottom = false, left = false, right = false;
        for (var a = 0; a < h; a++)
            for (var b = 0; b < w; b++)
                if (fill[a * w + b] == 1) {
                    if (a == 0) top = true;
                    if (a == h - 1) bottom = true;
                    if (b == 0) left = true;
                    if (b == w - 1) right = true;
                }
        return top && bottom && left && right;
    }

    private int[] place(CellSet s, int row, int col) {
        final var g = new int[inRows * inCols];
        for (var i = 0; i < s.size(); i++) g[(row + s.a(i)) * inCols + col + s.b(i)] = 1;
        return g;
    }

    private CellSet cropOf(int[] grid) {
        final var as = new ArrayList<Integer>();
        final var bs = new ArrayList<Integer>();
        for (var i = 0; i < grid.length; i++) if (grid[i] == 1) { as.add(i / inCols); bs.add(i % inCols); }
        return CellSet.of(as.stream().mapToInt(Integer::intValue).toArray(), bs.stream().mapToInt(Integer::intValue).toArray());
    }

    // ---- inspection ---------------------------------------------------------------------------------

    /** Both tries drawn as indented paths, each node with its counts and leaves marked. */
    public String render() {
        final var sb = new StringBuilder();
        for (final var root : List.of(d4, d6)) {
            sb.append(root == d4 ? "D4 root" : "D6 root")
              .append(String.format("  [neg %d, pos %d]%n", root.counts[0], root.counts[1]));
            for (final var c : root.children.values()) draw(c, "  ", sb);
        }
        return sb.toString();
    }

    private static void draw(Node n, String indent, StringBuilder sb) {
        sb.append(indent).append(step(n)).append(String.format("  [neg %d, pos %d]%s%n",
                n.counts[0], n.counts[1], n.splitOn == null ? "  leaf" : ""));
        for (final var c : n.children.values()) draw(c, indent + "  ", sb);
    }

    /** Nodes in both tries (roots included). */
    public int size() { return count(d4) + count(d6); }

    private static int count(Node n) {
        var k = 1;
        for (final var c : n.children.values()) k += count(c);
        return k;
    }

    /** The extension primitives currently inserted anywhere, by name. */
    public Set<String> extensions() {
        final var out = new LinkedHashSet<String>();
        for (final var root : List.of(d4, d6)) extensionsUnder(root, out);
        return out;
    }

    private static void extensionsUnder(Node n, Set<String> out) {
        if (n.splitOn != null && n.splitOn.level() == Level.EXTENSION) out.add(n.splitOn.primitive().name());
        for (final var c : n.children.values()) extensionsUnder(c, out);
    }
}
