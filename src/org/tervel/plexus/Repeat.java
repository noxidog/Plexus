package org.tervel.plexus;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * How a grid of cells <b>repeats</b>: the whole shifts under which it maps onto itself. Where {@link Descriptor}
 * asks which turns and mirrors leave a shape unchanged, this asks which translations leave a pattern unchanged,
 * and together they are the pattern's symmetry: a lattice of shifts, and the symmetry of the motif it repeats.
 *
 * <p>The grid is a binary or label grid, row-major: 0 is background, any other value a label. A repeating grid
 * splits into a <b>frame</b> and its <b>defects</b> — the motif stamped at every lattice point, and whatever
 * breaks that — which {@link #periods} finds. A grid that does not repeat is the trivial case: no basis, and its
 * period is its own box. Everything here is exact: steps are whole, agreement is a count, and a defect is a period
 * that differs.
 *
 * <h2>The basis</h2>
 * A shift's <b>agreement</b> is the share of labelled cells whose cell one shift over holds the same label, and its
 * <b>prominence</b> how far that stands from chance towards perfect agreement: chance being how often the labels
 * would agree if they were scattered independently (each label's share of the labelled cells times its share of
 * the frame, summed). The repeats are the shifts where agreement peaks: the zero shift is a hill of its own (a shape
 * always overlaps itself a little one cell over), so only shifts at least as agreeing as all their neighbours
 * count, and only at a prominence of {@value #PROMINENCE} or more: relative, because a pattern filling a third of
 * the frame agrees far less overall than one filling all of it, and random cells agree as often as they are dense.
 * The best repeat and the best not parallel to it span the lattice; a repeat that is a whole multiple of a shorter
 * one at least half as prominent is taken as that shorter one.
 *
 * <p>A grid can also repeat every single cell: one label filling it, the rest defects. A one-cell step sits beside
 * the zero shift, which always agrees perfectly, so it is never a peak. So where no peak is found, the grid repeats
 * every cell when one label clearly fills it (more than half the frame, and more than twice any other label), and
 * does not repeat otherwise: a lone shape in an empty frame is not a pattern, and neither are random cells that
 * happen to be one label a little over half the time. The pair is then reduced to the shortest pair spanning the same lattice and
 * oriented by a fixed rule, so the same pattern always gets the same {@code (i, j)} addresses:
 * <ol>
 *   <li>{@code v1} is the shortest repeat and {@code v2} the shortest not parallel to it (Lagrange–Gauss);</li>
 *   <li>when they are equally long {@code v1} is the more horizontal;</li>
 *   <li>{@code v1} points right ({@code x > 0}, or straight down), {@code v2} points down ({@code y > 0});</li>
 *   <li>on a hex lattice {@code v2} is the neighbour at 60° to {@code v1}, never the one at 120°.</li>
 * </ol>
 *
 * <h2>The type and its moves</h2>
 * Lengths and angles are read in the grid's storage: a square grid's own, or for a hex grid stored axially
 * ({@link Lattice#HEX_6}) the hex plane's, where a hex lattice is exact. The basis says which of the five plane
 * lattices this is, and so which moves of the index grid map it onto itself, as subsets of Plexus's own move
 * tables in their own order:
 *
 * <table>
 *   <tr><th>type</th><th>basis</th><th>moves</th></tr>
 *   <tr><td>SQUARE</td><td>equal, 90°</td><td>D4 (8)</td></tr>
 *   <tr><td>HEX</td><td>equal, 60°</td><td>D6 (12)</td></tr>
 *   <tr><td>RECTANGULAR</td><td>unequal, 90°</td><td>identity, rot180, mirrorH, mirrorV</td></tr>
 *   <tr><td>RHOMBIC</td><td>equal, any other angle</td><td>identity, rot180, diagMain, diagAnti</td></tr>
 *   <tr><td>OBLIQUE</td><td>unequal, any angle</td><td>identity, rot180</td></tr>
 * </table>
 *
 * The square family's moves act on the index grid as {@code (row, column) = (j, i)}; hex's as axial
 * {@code (q, r) = (i, j)}.
 *
 * @param type      which lattice the repeat is.
 * @param storage   the lattice the grid is stored on: square, or hex stored axially.
 * @param v1        the first step, in cells; {@code null} when nothing repeats.
 * @param v2        the second; {@code null} unless the repeat is two-dimensional.
 * @param strength1 the agreement one {@code v1} over, as a share of the labelled cells.
 * @param strength2 likewise for {@code v2}.
 */
public record Repeat(Type type, Lattice storage, Step v1, Step v2, double strength1, double strength2) {

    /** The plane lattices, plus the two degenerate answers. */
    public enum Type { NONE, STRIPE, SQUARE, RECTANGULAR, RHOMBIC, HEX, OBLIQUE }

    /** A whole step, in cells: {@code x} along a row, {@code y} down the rows. */
    public record Step(int x, int y) {
        public Step plus(Step o) { return new Step(x + o.x, y + o.y); }
        public Step minus(Step o) { return new Step(x - o.x, y - o.y); }
        public Step times(int k) { return new Step(x * k, y * k); }
        long cross(Step o) { return (long) x * o.y - (long) y * o.x; }
    }

    /** Nothing repeats. */
    public static Repeat none(Lattice storage) {
        return new Repeat(Type.NONE, storage, null, null, 0, 0);
    }

    /** How a grid stored on the square lattice repeats. */
    public static Repeat of(int[] grid, int rows, int cols) {
        return of(grid, rows, cols, Lattice.SQUARE_4);
    }

    /**
     * How {@code grid} ({@code rows × cols} labels, row-major, 0 background) repeats, with lengths and angles read
     * on {@code storage}: the square lattice, or the hex lattice stored axially.
     */
    public static Repeat of(int[] grid, int rows, int cols, Lattice storage) {
        if (grid.length != rows * cols)
            throw new IllegalArgumentException("grid has " + grid.length + " cells, frame is " + rows + "x" + cols);
        final var agree = agreement(grid, rows, cols);
        if (agree == null) return none(storage);
        final var peaks = new ArrayList<int[]>();
        for (var dy = 0; dy <= rows / 2; dy++)
            for (var dx = -(cols / 2); dx <= cols / 2; dx++) {
                if (dy == 0 && dx <= 0) continue;
                final var a = agree.at(dx, dy);
                if (agree.prominence(dx, dy) < PROMINENCE) continue;
                var peak = true;
                for (var ny = -1; ny <= 1 && peak; ny++)
                    for (var nx = -1; nx <= 1; nx++)
                        if ((nx != 0 || ny != 0) && agree.at(dx + nx, dy + ny) > a) { peak = false; break; }
                if (peak) peaks.add(new int[]{dx, dy});
            }
        if (peaks.isEmpty()) {
            final var counts = new java.util.HashMap<Integer, Integer>();
            for (final var v : grid) counts.merge(v, 1, Integer::sum);
            final var sorted = counts.entrySet().stream()
                    .sorted(java.util.Map.Entry.<Integer, Integer>comparingByValue().reversed()).toList();
            final var fills = sorted.get(0);
            final var next = sorted.size() > 1 ? sorted.get(1).getValue() : 0;
            if (fills.getKey() == 0 || 2 * fills.getValue() <= grid.length || fills.getValue() <= 2 * next)
                return none(storage);
            return basis(new Step(1, 0), new Step(0, 1), storage, agree.at(1, 0), agree.at(0, 1));
        }
        final java.util.Comparator<int[]> best = (p, q) -> {
            final var c = Double.compare(agree.at(q[0], q[1]), agree.at(p[0], p[1]));
            return c != 0 ? c : Long.compare(norm(storage, new Step(p[0], p[1])), norm(storage, new Step(q[0], q[1])));
        };
        peaks.sort(best);
        final var first = shortest(agree, peaks.get(0));
        int[] second = null;
        for (final var p : peaks)
            if ((long) first[0] * p[1] - (long) first[1] * p[0] != 0) { second = shortest(agree, p); break; }
        if (second == null) {
            final var v = orientRight(new Step(first[0], first[1]));
            return new Repeat(Type.STRIPE, storage, v, null, agree.at(first[0], first[1]), 0);
        }
        return basis(new Step(first[0], first[1]), new Step(second[0], second[1]), storage,
                agree.at(first[0], first[1]), agree.at(second[0], second[1]));
    }

    /**
     * A repeat with this basis on {@code storage}: reduced to the shortest pair, oriented, and classified, by the
     * rules in the class description.
     */
    public static Repeat basis(Step a, Step b, Lattice storage, double sa, double sb) {
        if (a.cross(b) == 0) throw new IllegalArgumentException("parallel steps span no lattice: " + a + ", " + b);
        for (var guard = 0; guard < 64; guard++) {                        // Lagrange–Gauss reduction
            if (norm(storage, b) < norm(storage, a)) {
                final var t = a; a = b; b = t;
                final var s = sa; sa = sb; sb = s;
            }
            final var m = Math.round((double) dot(storage, a, b) / norm(storage, a));
            if (m == 0) break;
            b = b.minus(a.times((int) m));
        }
        if (norm(storage, a) == norm(storage, b)
                && Math.abs((double) b.y()) * len(a) < Math.abs((double) a.y()) * len(b)) {
            final var t = a; a = b; b = t;
            final var s = sa; sa = sb; sb = s;
        }
        a = orientRight(a);
        b = orientDown(b);
        final var na = norm(storage, a);
        final var nb = norm(storage, b);
        final var d = dot(storage, a, b);
        final Type type;
        if (na == nb && d == 0) type = Type.SQUARE;
        else if (d == 0) type = Type.RECTANGULAR;
        else if (na == nb && 2 * Math.abs(d) == na) {
            type = Type.HEX;
            if (d < 0) b = orientDown(b.plus(a));                          // the 60° neighbour, not the 120° one
        } else if (na == nb) type = Type.RHOMBIC;
        else type = Type.OBLIQUE;
        return new Repeat(type, storage, a, b, sa, sb);
    }

    /** The same basis read as {@code declared}: what a caller that fixed its lattice in advance works with. */
    public Repeat as(Type declared) {
        return new Repeat(declared, storage, v1, v2, strength1, strength2);
    }

    /**
     * The Plexus lattice the index grid is addressed on: hex for {@link Type#HEX}, square for the rest of the plane
     * lattices (whose own moves {@link #moves} restricts), none for a stripe or nothing.
     */
    public Optional<Lattice> lattice() {
        return switch (type) {
            case NONE, STRIPE -> Optional.empty();
            case HEX -> Optional.of(Lattice.HEX_6);
            default -> Optional.of(Lattice.SQUARE_4);
        };
    }

    /** The names of the moves that map this lattice onto itself, in Plexus's own order. */
    public List<String> moves() {
        return maps().stream().map(CellSet.Map::name).toList();
    }

    /** Those moves as cell maps on the index grid. */
    List<CellSet.Map> maps() {
        return switch (type) {
            case NONE, STRIPE -> List.of();
            case SQUARE -> Groups.D4;
            case HEX -> Groups.D6;
            case RECTANGULAR -> pick(Groups.D4, "identity", "rot180", "mirrorH", "mirrorV");
            case RHOMBIC -> pick(Groups.D4, "identity", "rot180", "diagMain", "diagAnti");
            case OBLIQUE -> pick(Groups.D4, "identity", "rot180");
        };
    }

    /**
     * Each move as a 2×2 integer matrix on the grid's own {@code (x, y)} cell steps, row-major: the index-grid move
     * carried through the basis, {@code B · M · B⁻¹}. Exact for the moves of the lattice's type.
     */
    int[][] cellMatrices() {
        final var maps = maps();
        final var out = new int[maps.size()][];
        final double det = v1.cross(v2);
        for (var k = 0; k < maps.size(); k++) {
            final var g = maps.get(k);
            final int m00, m01, m10, m11;                                  // on (i, j)
            if (type == Type.HEX) {
                m00 = g.fa().applyAsInt(1, 0); m01 = g.fa().applyAsInt(0, 1);
                m10 = g.fb().applyAsInt(1, 0); m11 = g.fb().applyAsInt(0, 1);
            } else {                                                       // (a, b) = (row, column) = (j, i)
                m00 = g.fb().applyAsInt(0, 1); m01 = g.fb().applyAsInt(1, 0);
                m10 = g.fa().applyAsInt(0, 1); m11 = g.fa().applyAsInt(1, 0);
            }
            final double b00 = v1.x() * m00 + v2.x() * m10, b01 = v1.x() * m01 + v2.x() * m11;
            final double b10 = v1.y() * m00 + v2.y() * m10, b11 = v1.y() * m01 + v2.y() * m11;
            final double i00 = v2.y() / det, i01 = -v2.x() / det, i10 = -v1.y() / det, i11 = v1.x() / det;
            out[k] = new int[]{(int) Math.round(b00 * i00 + b01 * i10), (int) Math.round(b00 * i01 + b01 * i11),
                    (int) Math.round(b10 * i00 + b11 * i10), (int) Math.round(b10 * i01 + b11 * i11)};
        }
        return out;
    }

    /**
     * Where the repeat holds in {@code grid}, the motif it repeats, and what breaks it. See {@link Periods}.
     *
     * @param sizes  the blocks the region may be, as {@code {cols, rows}} in periods; empty to take the largest
     *               block that repeats.
     * @param picker which of the motif's equally symmetric positions is a cell; {@link Periods#NEAREST_TOP_LEFT}
     *               when the caller has no way to tell.
     * @return {@code null} when this repeat is not two-dimensional or no block of the sizes stands out.
     */
    public Periods periods(int[] grid, int rows, int cols, List<int[]> sizes, Periods.Picker picker) {
        return Periods.of(this, grid, rows, cols, sizes, picker);
    }

    // ---- measurement --------------------------------------------------------------------------------------

    /** Agreement at every shift, exact: matching labelled cells over labelled cells with a partner in the frame. */
    private static final class Agreement {
        private final long[][] match;
        private final long[][] count;
        private final int rows;
        private final int cols;
        /** How often the labels would agree if they were scattered independently. */
        double chance;

        Agreement(long[][] match, long[][] count, int rows, int cols) {
            this.match = match;
            this.count = count;
            this.rows = rows;
            this.cols = cols;
        }

        double at(int dx, int dy) {
            if (Math.abs(dx) >= cols || Math.abs(dy) >= rows) return 0;
            final var h = match.length;
            final var w = match[0].length;
            final var c = count[Math.floorMod(dy, h)][Math.floorMod(dx, w)];
            return c == 0 ? 0 : (double) match[Math.floorMod(dy, h)][Math.floorMod(dx, w)] / c;
        }

        /** How far the shift's agreement stands from chance towards perfect; 1 for every shift of a uniform grid. */
        double prominence(int dx, int dy) {
            return chance >= 1 ? 1 : (at(dx, dy) - chance) / (1 - chance);
        }
    }

    /**
     * For every shift {@code s}: how many labelled cells {@code p} have {@code p + s} in the frame holding the same
     * label, and how many have {@code p + s} in the frame at all. Counted through the FFT (one correlation per
     * label, padded so nothing wraps) and rounded, so the counts are exact. {@code null} for a grid with no label.
     */
    private static Agreement agreement(int[] grid, int rows, int cols) {
        final var labels = new java.util.TreeSet<Integer>();
        for (final var v : grid) if (v != 0) labels.add(v);
        if (labels.isEmpty()) return null;
        if (labels.size() > MAX_LABELS)
            throw new IllegalArgumentException("a grid of " + labels.size() + " labels is a picture, not a grid of"
                    + " cells: read it into cells first");
        final var h = Fft.pow2(2 * rows);
        final var w = Fft.pow2(2 * cols);
        final var match = new long[h][w];
        for (final var label : labels) {
            final var a = new double[h][w];
            for (var y = 0; y < rows; y++) for (var x = 0; x < cols; x++) if (grid[y * cols + x] == label) a[y][x] = 1;
            add(match, cross(a, copy(a)));
        }
        final var labelled = new double[h][w];
        final var frame = new double[h][w];
        for (var y = 0; y < rows; y++)
            for (var x = 0; x < cols; x++) { labelled[y][x] = grid[y * cols + x] != 0 ? 1 : 0; frame[y][x] = 1; }
        final var agree = new Agreement(match, cross(labelled, frame), rows, cols);
        final var counts = new java.util.HashMap<Integer, Long>();
        for (final var v : grid) if (v != 0) counts.merge(v, 1L, Long::sum);
        final double all = grid.length;
        final double total = counts.values().stream().mapToLong(Long::longValue).sum();
        for (final var n : counts.values()) agree.chance += (n / total) * (n / all);
        return agree;
    }

    /** {@code Σ_p a[p]·b[p+s]} for every shift {@code s}, rounded; both arrays are consumed. */
    private static long[][] cross(double[][] a, double[][] b) {
        final var h = a.length;
        final var w = a[0].length;
        final var ai = new double[h][w];
        final var bi = new double[h][w];
        Fft.transform(a, ai, false);
        Fft.transform(b, bi, false);
        for (var y = 0; y < h; y++)
            for (var x = 0; x < w; x++) {                                  // conj(A) · B
                final var re = a[y][x] * b[y][x] + ai[y][x] * bi[y][x];
                final var im = a[y][x] * bi[y][x] - ai[y][x] * b[y][x];
                a[y][x] = re;
                ai[y][x] = im;
            }
        Fft.transform(a, ai, true);
        final var out = new long[h][w];
        for (var y = 0; y < h; y++) for (var x = 0; x < w; x++) out[y][x] = Math.round(a[y][x]);
        return out;
    }

    private static double[][] copy(double[][] m) {
        final var out = new double[m.length][];
        for (var y = 0; y < m.length; y++) out[y] = m[y].clone();
        return out;
    }

    private static void add(long[][] into, long[][] m) {
        for (var y = 0; y < m.length; y++) for (var x = 0; x < m[0].length; x++) into[y][x] += m[y][x];
    }

    /**
     * The shortest whole divisor of repeat {@code p} that is itself a repeat: {@code d = p / k} for the largest {@code k}
     * at which every multiple {@code d, 2d, …, kd} is at least half as prominent as {@code p}. A multiple can agree a
     * little better than the true step by chance (a shift that happens to carry every defect onto background), never
     * by half; and a short shift that agrees only because cells clump together fails somewhere along the chain.
     */
    private static int[] shortest(Agreement agree, int[] p) {
        final var g = gcd(Math.abs(p[0]), Math.abs(p[1]));
        final var floor = Math.max(PROMINENCE, agree.prominence(p[0], p[1]) / 2);
        for (var k = g; k >= 2; k--) {
            if (g % k != 0) continue;
            final int dx = p[0] / k, dy = p[1] / k;
            var chain = true;
            for (var m = 1; m < k && chain; m++) chain = agree.prominence(m * dx, m * dy) >= floor;
            if (chain) return new int[]{dx, dy};
        }
        return p;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? Math.max(a, 1) : gcd(b, a % b);
    }

    /** {@code 2·(a · b)} on {@code storage}: a square grid's own dot product, or the hex plane's for axial steps. */
    static long dot(Lattice storage, Step a, Step b) {
        final long p = (long) a.x() * b.x() + (long) a.y() * b.y();
        return storage == Lattice.HEX_6 ? 2 * p + (long) a.x() * b.y() + (long) a.y() * b.x() : 2 * p;
    }

    /** {@code 2·|a|²} on {@code storage}. */
    static long norm(Lattice storage, Step a) {
        return dot(storage, a, a);
    }

    /** {@code |a|} on the square grid, for comparing how horizontal two equal steps are. */
    private static double len(Step a) {
        return Math.hypot(a.x(), a.y());
    }

    private static Step orientRight(Step v) {
        return v.x() < 0 || (v.x() == 0 && v.y() < 0) ? v.times(-1) : v;
    }

    private static Step orientDown(Step v) {
        return v.y() < 0 || (v.y() == 0 && v.x() < 0) ? v.times(-1) : v;
    }

    private static List<CellSet.Map> pick(List<CellSet.Map> group, String... names) {
        final var out = new ArrayList<CellSet.Map>();
        for (final var g : group) for (final var n : names) if (g.name().equals(n)) out.add(g);
        return List.copyOf(out);
    }

    /**
     * The least prominence a shift needs to be a repeat: a quarter of the way from chance to perfect agreement.
     * Measured: a board filling a third of a frame of 30% random cells stands at 35%, a tiled grid with two defects at
     * over 99%, and random cells' best shift at 13%.
     */
    static final double PROMINENCE = 0.25;

    /** The most labels a grid of cells may carry; more is a picture of pixels, which is read into cells first. */
    static final int MAX_LABELS = 64;
}
