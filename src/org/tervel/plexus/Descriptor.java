package org.tervel.plexus;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the router reads off one input grid, computed once.
 *
 * <ol>
 *   <li>The bounding box of the non-zero pixels on the square input. An empty grid has none.</li>
 *   <li>The lattice. A rectangle stays square. A square box is tried on the hex lattice when a diagonal's
 *       width equals its side (row + column for a right shift of the rows, row − column for a left shift; both
 *       → keep the shift with the smaller form up to D6); hex wins only if it has strictly more symmetry.</li>
 *   <li>The canonical pose. A rectangle is turned to landscape (the envelope), then the smallest of its four
 *       D2 images is taken. A square box or hex shape takes the pose whose walk answers say yes as early as
 *       possible, ties to the smallest image.</li>
 *   <li>The walk answers, mass and joins on the canonical pose, and the trailing coordinates: orientation
 *       (envelope turn, or shift direction), the pose code (the lowest-numbered transform reaching the
 *       canonical pose), and the raw top-left corner of the box in the input frame.</li>
 * </ol>
 *
 * @param kind        what the region is ({@link Kind})
 * @param root        which trie it is filed under
 * @param boxShort    short side of the box (the common extent on hex; 0 when empty)
 * @param boxLong     long side of the box (equal to {@code boxShort} for square boxes and hex)
 * @param answers     the symmetry walk answers, in {@link #walkNames()} order
 * @param mass        filled pixels
 * @param joins       adjacent pairs on the shape's own lattice (4 or 8 neighbours square, 6 hex)
 * @param orientation envelope turn (rectangle) or shift direction (hex: 0 right, 1 left); −1 when not used
 * @param pose        index of the transform that reached the canonical pose
 * @param cornerRow   top row of the box in the input frame
 * @param cornerCol   left column of the box in the input frame
 * @param canonical   the shape in its canonical pose, in its lattice's coordinates (package use)
 * @param lattice     the lattice that described it, which fixes what {@code joins} counts
 * @param cropped     the shape cropped to its box, before any turn (package use)
 * @param rows        rows of the input frame
 * @param cols        columns of the input frame
 */
public record Descriptor(Kind kind, Root root, int boxShort, int boxLong, boolean[] answers, int mass, int joins,
                         int orientation, int pose, int cornerRow, int cornerCol, CellSet canonical,
                         Lattice lattice, CellSet cropped, int rows, int cols) {

    /** The names of the walk's questions, in order. */
    public List<String> walkNames() { return Groups.walkFor(kind).stream().map(CellSet.Map::name).toList(); }

    /** The symmetries the walk answered yes to (identity omitted). */
    public List<String> symmetries() {
        final var names = walkNames();
        final var out = new ArrayList<String>();
        for (var i = 0; i < answers.length; i++) if (answers[i]) out.add(names.get(i));
        return out;
    }

    /**
     * How many motions of the shape's lattice leave it unchanged, wherever it sits: of the square's eight
     * for a square box (a rectangle's box admits only the four that keep it a rectangle), of the twelve of
     * D6 on hex. An empty grid is fixed by all eight.
     */
    public int stabilizerSize() {
        return switch (kind) {
            case EMPTY -> Groups.D4.size();
            case RECT -> Groups.stabilizerSize(canonical, Groups.D2);
            case SQUARE -> Groups.stabilizerSize(canonical, Groups.D4);
            case HEX -> Groups.stabilizerSize(canonical, Groups.D6);
        };
    }

    /** How many distinct orientations the shape has: the lattice's motions ÷ {@link #stabilizerSize}. */
    public int orbitSize() {
        return (kind == Kind.HEX ? Groups.D6.size() : Groups.D4.size()) / stabilizerSize();
    }

    /** Whether {@code other} is the same shape: equal canonical poses, so equal up to turns, mirrors and moves. */
    public boolean sameShape(Descriptor other) { return root == other.root && canonical.equals(other.canonical); }

    /** The shape-level part of the key (everything above the trailing coordinates), as a string, for tests. */
    public String shapeKey() {
        final var sb = new StringBuilder().append(root).append('|').append(lattice).append('|').append(kind).append('|')
                .append(boxShort).append('x').append(boxLong).append('|');
        for (final var a : answers) sb.append(a ? '1' : '0');
        return sb.append('|').append(mass).append('|').append(joins).toString();
    }

    // ---- companion planes ----------------------------------------------------------------------------

    /**
     * Carry {@code planes} into this shape's canonical frame: the planes are of the same input frame as the
     * grid this describes (companions such as the other faces of a board), and each is moved exactly as
     * the shape was — cropped at its corner, turned to landscape or sheared onto hex, then turned or
     * mirrored into the canonical pose. Every plane moves by the <b>same</b> motion, so where they sit
     * relative to the shape and to each other is kept.
     *
     * <p>When the shape has a symmetry, several motions reach its canonical pose and each puts the
     * companions somewhere different. All of them are tried and the smallest {@link Fold} is kept, so two
     * inputs that differ by a symmetry of the shape fold to the same result. On hex the same holds for the
     * shear: a shape that reads as the same hexagon sheared either way (a dot, the disk) is tried both
     * ways, since a quarter turn of the frame swaps the two diagonals and so the two shears.
     *
     * <p>An empty shape has no canonical pose; the planes come back unmoved, in input coordinates.
     *
     * @param planes binary planes of {@code rows × cols} cells (1 filled)
     * @throws IllegalArgumentException if a plane is not of this frame
     */
    public Fold toCanonical(int[]... planes) {
        final var as = new int[planes.length][];
        final var bs = new int[planes.length][];
        for (var p = 0; p < planes.length; p++) {
            if (planes[p].length != rows * cols)
                throw new IllegalArgumentException("plane " + p + " has " + planes[p].length + " cells, frame is "
                        + rows + "x" + cols);
            var n = 0;
            for (final var v : planes[p]) if (v == 1) n++;
            as[p] = new int[n];
            bs[p] = new int[n];
            var k = 0;
            for (var i = 0; i < planes[p].length; i++) {
                if (planes[p][i] != 1) continue;
                as[p][k] = i / cols - cornerRow;
                bs[p][k] = i % cols - cornerCol;
                k++;
            }
        }
        if (kind == Kind.EMPTY) {
            for (var p = 0; p < planes.length; p++)
                for (var i = 0; i < as[p].length; i++) { as[p][i] += cornerRow; bs[p][i] += cornerCol; }
            return Fold.of(as, bs);
        }

        final var group = kind == Kind.RECT ? Groups.D2 : kind == Kind.SQUARE ? Groups.D4 : Groups.D6;
        final var lifts = kind == Kind.HEX ? new int[]{0, 1} : new int[]{orientation};
        Fold best = null;
        for (final var lift : lifts) {
            final var refA = new int[cropped.size()];
            final var refB = new int[cropped.size()];
            for (var i = 0; i < refA.length; i++) { refA[i] = cropped.a(i); refB[i] = cropped.b(i); }
            lift(refA, refB, lift);
            final var la = new int[planes.length][];
            final var lb = new int[planes.length][];
            for (var p = 0; p < planes.length; p++) {
                la[p] = as[p].clone();
                lb[p] = bs[p].clone();
                lift(la[p], lb[p], lift);
            }
            for (final var g : group) {
                final var fold = foldUnder(g, refA, refB, la, lb);
                if (fold != null && (best == null || fold.compareTo(best) < 0)) best = fold;
            }
        }
        return best;
    }

    /**
     * The planes {@code (as, bs)} moved by {@code g} and shifted as the reference {@code (refA, refB)} is
     * shifted onto its canonical pose; null when {@code g} does not carry the reference there.
     */
    private Fold foldUnder(CellSet.Map g, int[] refA, int[] refB, int[][] as, int[][] bs) {
        final var ra = new int[refA.length];
        final var rb = new int[refA.length];
        var minA = Integer.MAX_VALUE;
        var minB = Integer.MAX_VALUE;
        for (var i = 0; i < ra.length; i++) {
            ra[i] = g.fa().applyAsInt(refA[i], refB[i]);
            rb[i] = g.fb().applyAsInt(refA[i], refB[i]);
            minA = Math.min(minA, ra[i]);
            minB = Math.min(minB, rb[i]);
        }
        if (!CellSet.of(ra, rb).equals(canonical)) return null;
        final var fa = new int[as.length][];
        final var fb = new int[as.length][];
        for (var p = 0; p < as.length; p++) {
            fa[p] = new int[as[p].length];
            fb[p] = new int[as[p].length];
            for (var i = 0; i < as[p].length; i++) {
                fa[p][i] = g.fa().applyAsInt(as[p][i], bs[p][i]) - minA;
                fb[p][i] = g.fb().applyAsInt(as[p][i], bs[p][i]) - minB;
            }
        }
        return Fold.of(fa, fb);
    }

    /**
     * Put cropped square cells {@code (row, column)} into the coordinates the walk ran in, in place: a
     * standing rectangle ({@code turn} 1) is turned to landscape, a hex shape is sheared to axial
     * {@code (q, r)} with its rows shifted right ({@code turn} 0) or left (1), and a square box is left as
     * it is.
     */
    private void lift(int[] as, int[] bs, int turn) {
        for (var i = 0; i < as.length; i++) {
            final int r = as[i], c = bs[i];
            switch (kind) {
                case RECT -> {
                    if (turn == 1) {
                        as[i] = Groups.D4.get(1).fa().applyAsInt(r, c);
                        bs[i] = Groups.D4.get(1).fb().applyAsInt(r, c);
                    }
                }
                case HEX -> { as[i] = turn == 1 ? -c : c; bs[i] = r; }
                default -> { }
            }
        }
    }

    // ---- computation ---------------------------------------------------------------------------------

    /** Describe an {@code rows×cols} input grid under the default lattices ({@link Lattice#DEFAULT}). */
    public static Descriptor of(int[] grid, int rows, int cols) { return of(grid, rows, cols, Lattice.DEFAULT); }

    /**
     * Describe an {@code rows×cols} input grid under {@code lattices}: the square one counts joins for every
     * shape that stays square, and the hex lattice is only tried if the set holds {@link Lattice#HEX_6}.
     *
     * @throws IllegalArgumentException if the set is not legal ({@link Lattice#checked(Set)})
     */
    public static Descriptor of(int[] grid, int rows, int cols, java.util.Set<Lattice> lattices) {
        final var square = Lattice.square(Lattice.checked(lattices));
        final var hexAllowed = lattices.contains(Lattice.HEX_6);
        if (grid.length != rows * cols)
            throw new IllegalArgumentException("grid has " + grid.length + " cells, frame is " + rows + "x" + cols);
        final var rs = new ArrayList<Integer>();
        final var cs = new ArrayList<Integer>();
        for (var i = 0; i < grid.length; i++) if (grid[i] == 1) { rs.add(i / cols); cs.add(i % cols); }
        if (rs.isEmpty())
            return new Descriptor(Kind.EMPTY, Root.D4, 0, 0, new boolean[0], 0, 0, -1, 0, 0, 0,
                    CellSet.of(new int[0], new int[0]), square, CellSet.of(new int[0], new int[0]), rows, cols);

        final int minR = rs.stream().mapToInt(Integer::intValue).min().orElseThrow();
        final int minC = cs.stream().mapToInt(Integer::intValue).min().orElseThrow();
        final var ra = rs.stream().mapToInt(r -> r - minR).toArray();
        final var ca = cs.stream().mapToInt(c -> c - minC).toArray();
        final var shape = CellSet.of(ra, ca);
        final int h = shape.extentA, w = shape.extentB;
        final var mass = shape.size();

        if (h != w) {                                                     // rectangle: envelope, then D2
            final var orientation = h > w ? 1 : 0;
            final var env = orientation == 1 ? shape.map(Groups.D4.get(1)) : shape;
            final var canonical = Groups.orbitMin(env, Groups.D2);
            final var pose = firstReaching(env, canonical, Groups.D2);
            return new Descriptor(Kind.RECT, Root.D4, Math.min(h, w), Math.max(h, w),
                    answers(canonical, Groups.WALK_RECT), mass, canonical.edges(square.forward()),
                    orientation, pose, minR, minC, canonical, square, shape, rows, cols);
        }

        final var s = h;
        final var squareCount = Groups.stabilizerSize(shape, Groups.D4);
        CellSet hex = null;
        var direction = -1;
        if (hexAllowed && spread(ra, ca, +1) == s) { hex = shear(ra, ca, false); direction = 0; }
        if (hexAllowed && spread(ra, ca, -1) == s) {
            final var left = shear(ra, ca, true);
            if (hex == null || Groups.orbitMin(left, Groups.D6).compareTo(Groups.orbitMin(hex, Groups.D6)) < 0) {
                hex = left; direction = 1;
            }
        }
        if (hex != null && Groups.stabilizerSize(hex, Groups.D6) > squareCount) {
            final var canonical = earliestYes(hex, Groups.D6, Groups.WALK_HEX);
            return new Descriptor(Kind.HEX, Root.D6, s, s, answers(canonical, Groups.WALK_HEX), mass,
                    canonical.edges(Lattice.HEX_6.forward()), direction, firstReaching(hex, canonical, Groups.D6),
                    minR, minC, canonical, Lattice.HEX_6, shape, rows, cols);
        }
        final var canonical = earliestYes(shape, Groups.D4, Groups.WALK_SQUARE);
        return new Descriptor(Kind.SQUARE, Root.D4, s, s, answers(canonical, Groups.WALK_SQUARE), mass,
                canonical.edges(square.forward()), -1, firstReaching(shape, canonical, Groups.D4),
                minR, minC, canonical, square, shape, rows, cols);
    }

    /** Width of the shape across a diagonal: spread of row + column ({@code sign = +1}) or row − column (−1). */
    private static int spread(int[] rs, int[] cs, int sign) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (var i = 0; i < rs.length; i++) { final var v = rs[i] + sign * cs[i]; lo = Math.min(lo, v); hi = Math.max(hi, v); }
        return hi - lo + 1;
    }

    /**
     * Shear onto the hex lattice. Shifting the rows right reads square cell {@code (r, c)} as axial
     * {@code (q = c, r)}, which makes the anti-diagonal a neighbour direction; shifting left mirrors the columns
     * first, which makes the main diagonal one.
     */
    private static CellSet shear(int[] rs, int[] cs, boolean left) {
        final var q = new int[rs.length];
        for (var i = 0; i < rs.length; i++) q[i] = left ? -cs[i] : cs[i];
        return CellSet.of(q, rs);
    }

    /** The pose whose walk answers say yes earliest; ties to the smallest image. */
    private static CellSet earliestYes(CellSet shape, List<CellSet.Map> group, List<CellSet.Map> walk) {
        CellSet best = null;
        boolean[] bestAnswers = null;
        for (final var g : group) {
            final var img = shape.map(g);
            final var ans = answers(img, walk);
            final var cmp = bestAnswers == null ? -1 : compareAnswers(ans, bestAnswers);
            if (cmp < 0 || (cmp == 0 && img.compareTo(best) < 0)) { best = img; bestAnswers = ans; }
        }
        return best;
    }

    /** Negative when {@code x} says yes earlier than {@code y} at the first place they differ. */
    private static int compareAnswers(boolean[] x, boolean[] y) {
        for (var i = 0; i < x.length; i++) if (x[i] != y[i]) return x[i] ? -1 : 1;
        return 0;
    }

    static boolean[] answers(CellSet shape, List<CellSet.Map> walk) {
        final var out = new boolean[walk.size()];
        for (var i = 0; i < out.length; i++) out[i] = shape.map(walk.get(i)).equals(shape);
        return out;
    }

    /** The lowest-numbered transform carrying {@code from} onto {@code to}. */
    private static int firstReaching(CellSet from, CellSet to, List<CellSet.Map> group) {
        for (var i = 0; i < group.size(); i++) if (from.map(group.get(i)).equals(to)) return i;
        throw new IllegalStateException("canonical pose not reachable");
    }

    @Override public boolean equals(Object o) {
        return o instanceof Descriptor d && shapeKey().equals(d.shapeKey()) && orientation == d.orientation
                && pose == d.pose && cornerRow == d.cornerRow && cornerCol == d.cornerCol;
    }
    @Override public int hashCode() { return shapeKey().hashCode() * 31 + pose; }
}
