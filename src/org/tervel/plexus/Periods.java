package org.tervel.plexus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where a {@link Repeat} holds in a grid of cells, what it repeats, and what breaks it: the grid split into a
 * <b>frame</b> and its <b>defects</b>. Everything here is exact.
 *
 * <ul>
 *   <li><b>A period</b> is the {@code |v1 × v2|} cells the repeat's parallelogram covers from a corner; its
 *       {@link #cells} are their offsets from that corner, in reading order.</li>
 *   <li><b>The region</b> is a block of whole periods, {@code cols} along {@code v1} by {@code rows} along
 *       {@code v2}, its top-left period's corner at {@link #origin}.</li>
 *   <li><b>The motif</b> is the most frequent label at each of a period's cells over the region: what a period holds
 *       when nothing breaks it.</li>
 *   <li><b>A defect</b> is a period that differs from the motif anywhere; defects that hold exactly the same labels
 *       share a {@link #group}.</li>
 * </ul>
 *
 * <h2>Where a period starts</h2>
 * A repeat alone does not say where one period ends and the next begins: every one of its {@code |v1 × v2|}
 * corners tiles the grid. The motif's symmetry under the repeat's own moves ranks them ({@link #phases}): a corner
 * whose period is symmetric under more of the moves about its own centre is the more likely cell. Two can be
 * equally symmetric — a tile's centre and the crossing of the gaps between four tiles — so which is a cell is the
 * caller's to say ({@link Picker}); with nothing to tell them apart, {@link #NEAREST_TOP_LEFT}.
 *
 * <h2>Which block</h2>
 * Each period is scored by the share of its cells that hold the motif's label ({@link #score}, exact). With
 * {@code sizes} given, a block of each size is slid over the periods and the one whose inside stands highest above
 * the one-period ring round it wins, so the size is exact. Without, the periods split into two groups by score
 * (the value that best separates them, taken only when it explains at least {@value #TWO_GROUPS} of their spread,
 * above the 64% of noise round one value and the 75% of evenly spread values) and the region bounds the largest
 * connected run of the high group; when they form one group, the region is every whole period.
 *
 * @param repeat   the repeat this is of.
 * @param origin   the region's top-left period's corner, in cells.
 * @param cols     the region's width in periods, along {@code v1}.
 * @param rows     its height in periods, along {@code v2}.
 * @param phases   the candidate corners, most symmetric first.
 * @param phase    the one the picker chose.
 * @param cells    a period's cells, as offsets from its corner.
 * @param motif    the label at each of {@link #cells}.
 * @param score    each period's share of cells holding the motif's label, {@code [row][col]}.
 * @param distance how many of each period's cells differ from the motif.
 * @param defect   whether each period differs at all.
 * @param group    each defect's group, numbered from 1 in reading order; 0 for the motif.
 */
public record Periods(Repeat repeat, Repeat.Step origin, int cols, int rows,
                      List<Phase> phases, Phase phase, List<Repeat.Step> cells, int[] motif,
                      double[][] score, int[][] distance, boolean[][] defect, int[][] group) {

    /**
     * A candidate corner for the periods.
     *
     * @param corner a period corner, in cells, within the first period.
     * @param held   how many of the repeat's moves (identity included) the motif is symmetric under about the
     *               centre of the period that starts there.
     * @param moves  how many moves the repeat has.
     */
    public record Phase(Repeat.Step corner, int held, int moves) { }

    /** Chooses which candidate is a cell, given the motif read over the whole grid. */
    @FunctionalInterface
    public interface Picker {
        Phase pick(List<Phase> candidates, List<Repeat.Step> cells, int[] motif, Repeat repeat);
    }

    /** With nothing to tell the most symmetric candidates apart, the one nearest the top-left. */
    public static final Picker NEAREST_TOP_LEFT = (candidates, cells, motif, repeat) -> candidates.get(0);

    /** The corner of the period at {@code (row, col)} of the region, in cells. */
    public Repeat.Step corner(int row, int col) {
        return origin.plus(repeat.v1().times(col)).plus(repeat.v2().times(row));
    }

    /** The region's {@code {row, col}} whose period holds cell {@code (x, y)}, or {@code null} outside it. */
    public int[] cellAt(int x, int y) {
        final var ij = index(repeat, new Repeat.Step(x - origin.x(), y - origin.y()));
        return ij[0] < 0 || ij[1] < 0 || ij[0] >= cols || ij[1] >= rows ? null : new int[]{ij[1], ij[0]};
    }

    // ---- computation --------------------------------------------------------------------------------------

    static Periods of(Repeat repeat, int[] grid, int rows, int cols, List<int[]> sizes, Picker picker) {
        if (repeat.v2() == null) return null;
        if (grid.length != rows * cols)
            throw new IllegalArgumentException("grid has " + grid.length + " cells, frame is " + rows + "x" + cols);
        final var v1 = repeat.v1();
        final var v2 = repeat.v2();
        final var cells = domain(repeat);
        final var where = new HashMap<Long, Integer>();
        for (var k = 0; k < cells.size(); k++) where.put(key(cells.get(k)), k);

        // A first motif from the periods that agree with their neighbours: inside the pattern a period equals the one
        // beside it, in anything else it does not. Periods from corner (0, 0); the corner is found from this motif.
        final var rough = roughMotif(grid, rows, cols, repeat, cells);

        // Every corner, ranked by how symmetric the motif is about the centre of the period it starts.
        final var moves = repeat.cellMatrices();
        final var phases = new ArrayList<Phase>();
        for (final var o : cells) phases.add(new Phase(o, held(o, rough, cells, where, repeat, moves), moves.length));
        phases.sort((p, q) -> p.held() != q.held() ? Integer.compare(q.held(), p.held())
                : Long.compare(sq(p.corner()), sq(q.corner())));
        final var top = phases.get(0).held();
        final var candidates = phases.stream().filter(p -> p.held() == top).toList();
        final var phase = (picker == null ? NEAREST_TOP_LEFT : picker).pick(candidates, cells, rough, repeat);
        if (phase == null) return null;

        // Every whole period inside the frame at that corner, scored against the motif.
        final var o = phase.corner();
        final var range = range(repeat, o, rows, cols);
        final int i0 = range[0], j0 = range[1], ni = range[2], nj = range[3];
        final var periodScore = new double[nj][ni];
        for (var j = 0; j < nj; j++)
            for (var i = 0; i < ni; i++) {
                final var c = o.plus(v1.times(i0 + i)).plus(v2.times(j0 + j));
                periodScore[j][i] = inside(c, cells, rows, cols) ? match(grid, cols, c, cells, rough, where, repeat) : Double.NaN;
            }
        final var block = sizes == null || sizes.isEmpty() ? largestBlock(periodScore) : bestBlock(periodScore, sizes);
        if (block == null) return null;
        final int bi = block[0], bj = block[1], bc = block[2], br = block[3];
        final var origin = o.plus(v1.times(i0 + bi)).plus(v2.times(j0 + bj));

        // The motif over the region, and every period against it.
        final var block0 = new ArrayList<Repeat.Step>();
        for (var r = 0; r < br; r++) for (var c = 0; c < bc; c++) block0.add(origin.plus(v1.times(c)).plus(v2.times(r)));
        final var motif = mode(grid, cols, cells, block0);
        final var score = new double[br][bc];
        final var distance = new int[br][bc];
        final var defect = new boolean[br][bc];
        final var group = new int[br][bc];
        final var groups = new LinkedHashMap<String, Integer>();
        for (var r = 0; r < br; r++)
            for (var c = 0; c < bc; c++) {
                final var corner = origin.plus(v1.times(c)).plus(v2.times(r));
                final var labels = new int[cells.size()];
                var differ = 0;
                for (var k = 0; k < cells.size(); k++) {
                    final var p = corner.plus(cells.get(k));
                    labels[k] = grid[p.y() * cols + p.x()];
                    if (labels[k] != motif[k]) differ++;
                }
                distance[r][c] = differ;
                score[r][c] = 1 - (double) differ / cells.size();
                defect[r][c] = differ > 0;
                if (differ > 0) group[r][c] = groups.computeIfAbsent(Arrays.toString(labels), s -> groups.size() + 1);
            }
        return new Periods(repeat, origin, bc, br, List.copyOf(candidates), phase, cells, motif,
                score, distance, defect, group);
    }

    /** A period's cells from corner {@code (0, 0)}: the cells whose lattice coordinates both lie in {@code [0, 1)}. */
    private static List<Repeat.Step> domain(Repeat repeat) {
        final var v1 = repeat.v1();
        final var v2 = repeat.v2();
        final var xs = new int[]{0, v1.x(), v2.x(), v1.x() + v2.x()};
        final var ys = new int[]{0, v1.y(), v2.y(), v1.y() + v2.y()};
        final var out = new ArrayList<Repeat.Step>();
        for (var y = Arrays.stream(ys).min().orElse(0); y <= Arrays.stream(ys).max().orElse(0); y++)
            for (var x = Arrays.stream(xs).min().orElse(0); x <= Arrays.stream(xs).max().orElse(0); x++) {
                final var ij = index(repeat, new Repeat.Step(x, y));
                if (ij[0] == 0 && ij[1] == 0) out.add(new Repeat.Step(x, y));
            }
        return List.copyOf(out);
    }

    /** The whole lattice coordinates {@code {i, j}} of the period holding offset {@code p}: floors, exactly. */
    private static int[] index(Repeat repeat, Repeat.Step p) {
        final var v1 = repeat.v1();
        final var v2 = repeat.v2();
        final long det = v1.cross(v2);
        final long ni = (long) p.x() * v2.y() - (long) p.y() * v2.x();
        final long nj = (long) v1.x() * p.y() - (long) v1.y() * p.x();
        return new int[]{(int) Math.floorDiv(det > 0 ? ni : -ni, Math.abs(det)),
                (int) Math.floorDiv(det > 0 ? nj : -nj, Math.abs(det))};
    }

    /** The period class of offset {@code p} from corner {@code (0, 0)}: which of a period's cells it is. */
    private static int classOf(Repeat repeat, Map<Long, Integer> where, Repeat.Step p) {
        final var ij = index(repeat, p);
        final var r = p.minus(repeat.v1().times(ij[0])).minus(repeat.v2().times(ij[1]));
        return where.get(key(r));
    }

    private static long key(Repeat.Step s) {
        return ((long) s.x() << 32) ^ (s.y() & 0xffffffffL);
    }

    private static long sq(Repeat.Step s) {
        return (long) s.x() * s.x() + (long) s.y() * s.y();
    }

    /**
     * The motif from corner {@code (0, 0)}'s periods that hold a label and agree best with their neighbours (a period of
     * background alone is not pattern, however well it agrees): each whole period scored by
     * the share of its cells equal to the period one step over, both ways along both steps; the most frequent label
     * per cell over the periods above the best two-group cut of those scores (all of them when they are one value).
     */
    private static int[] roughMotif(int[] grid, int rows, int cols, Repeat repeat, List<Repeat.Step> cells) {
        final var zero = new Repeat.Step(0, 0);
        final var range = range(repeat, zero, rows, cols);
        final var corners = new ArrayList<Repeat.Step>();
        final var agreement = new ArrayList<Double>();
        final var steps = new Repeat.Step[]{repeat.v1(), repeat.v1().times(-1), repeat.v2(), repeat.v2().times(-1)};
        for (var j = 0; j < range[3]; j++)
            for (var i = 0; i < range[2]; i++) {
                final var c = repeat.v1().times(range[0] + i).plus(repeat.v2().times(range[1] + j));
                if (!inside(c, cells, rows, cols)) continue;
                var labelled = false;                                       // a period of background is not pattern
                for (final var k : cells) { final var a = c.plus(k); if (grid[a.y() * cols + a.x()] != 0) { labelled = true; break; } }
                if (!labelled) continue;
                var same = 0;
                var seen = 0;
                for (final var step : steps) {
                    final var n = c.plus(step);
                    if (!inside(n, cells, rows, cols)) continue;
                    for (final var k : cells) {
                        final var a = c.plus(k);
                        final var b = n.plus(k);
                        if (grid[a.y() * cols + a.x()] == grid[b.y() * cols + b.x()]) same++;
                        seen++;
                    }
                }
                if (seen == 0) continue;
                corners.add(c);
                agreement.add((double) same / seen);
            }
        final var values = agreement.stream().mapToDouble(Double::doubleValue).toArray();
        final var floor = cut(values);
        final var chosen = new ArrayList<Repeat.Step>();
        for (var k = 0; k < corners.size(); k++)
            if (Double.isNaN(floor) || values[k] >= floor) chosen.add(corners.get(k));
        return mode(grid, cols, cells, chosen);
    }

    /** The most frequent label at each period cell over the periods at {@code corners}; ties go to the smaller label. */
    private static int[] mode(int[] grid, int cols, List<Repeat.Step> cells, List<Repeat.Step> corners) {
        final var counts = new ArrayList<Map<Integer, Integer>>();
        for (var k = 0; k < cells.size(); k++) counts.add(new HashMap<>());
        for (final var corner : corners)
            for (var k = 0; k < cells.size(); k++) {
                final var p = corner.plus(cells.get(k));
                counts.get(k).merge(grid[p.y() * cols + p.x()], 1, Integer::sum);
            }
        final var out = new int[cells.size()];
        for (var k = 0; k < out.length; k++) {
            var best = 0;
            var most = -1;
            for (final var e : counts.get(k).entrySet())
                if (e.getValue() > most || (e.getValue() == most && e.getKey() < best)) { most = e.getValue(); best = e.getKey(); }
            out[k] = best;
        }
        return out;
    }

    /**
     * How many of the repeat's moves leave the motif unchanged about the centre of the period that starts at
     * {@code o}: each cell's centre carried through the move must land on a cell's centre holding the same label.
     * In doubled coordinates, so a centre half a cell off is still a whole number.
     */
    private static int held(Repeat.Step o, int[] motif, List<Repeat.Step> cells, Map<Long, Integer> where,
                            Repeat repeat, int[][] moves) {
        final var cx = 2L * o.x() + repeat.v1().x() + repeat.v2().x();      // 2 × the period's centre
        final var cy = 2L * o.y() + repeat.v1().y() + repeat.v2().y();
        var held = 0;
        for (final var m : moves) {
            var ok = true;
            for (final var r : cells) {
                final long px = 2L * (o.x() + r.x()) + 1 - cx;
                final long py = 2L * (o.y() + r.y()) + 1 - cy;
                final long ix = cx + m[0] * px + m[1] * py;
                final long iy = cy + m[2] * px + m[3] * py;
                if ((ix & 1) == 0 || (iy & 1) == 0) { ok = false; break; }   // lands between cells
                final var image = new Repeat.Step((int) ((ix - 1) / 2), (int) ((iy - 1) / 2));
                final var from = classOf(repeat, where, o.plus(r));
                if (motif[from] != motif[classOf(repeat, where, image)]) { ok = false; break; }
            }
            if (ok) held++;
        }
        return held;
    }

    /** {@code {i0, j0, ni, nj}}: the lattice indices whose periods can touch the frame, from corner {@code o}. */
    private static int[] range(Repeat repeat, Repeat.Step o, int rows, int cols) {
        int imin = Integer.MAX_VALUE, imax = Integer.MIN_VALUE, jmin = Integer.MAX_VALUE, jmax = Integer.MIN_VALUE;
        for (final var p : new int[][]{{0, 0}, {cols, 0}, {0, rows}, {cols, rows}}) {
            final var ij = index(repeat, new Repeat.Step(p[0] - o.x(), p[1] - o.y()));
            imin = Math.min(imin, ij[0]); imax = Math.max(imax, ij[0]);
            jmin = Math.min(jmin, ij[1]); jmax = Math.max(jmax, ij[1]);
        }
        return new int[]{imin, jmin, imax - imin + 1, jmax - jmin + 1};
    }

    private static boolean inside(Repeat.Step corner, List<Repeat.Step> cells, int rows, int cols) {
        for (final var c : cells) {
            final var p = corner.plus(c);
            if (p.x() < 0 || p.y() < 0 || p.x() >= cols || p.y() >= rows) return false;
        }
        return true;
    }

    /** The share of the period at {@code corner} whose cells hold the motif's label. */
    private static double match(int[] grid, int cols, Repeat.Step corner, List<Repeat.Step> cells, int[] motif,
                                Map<Long, Integer> where, Repeat repeat) {
        var same = 0;
        for (final var c : cells) {
            final var p = corner.plus(c);
            if (grid[p.y() * cols + p.x()] == motif[classOf(repeat, where, p)]) same++;
        }
        return (double) same / cells.size();
    }

    /**
     * {@code {i, j, cols, rows}} of the block of each allowed size whose inside stands highest above the one-period
     * ring round it. A ring period off the frame, or not whole inside it, is left out of the ring's mean, so a block
     * at the frame's edge is judged by the ring it has.
     */
    private static int[] bestBlock(double[][] s, List<int[]> sizes) {
        final var nj = s.length;
        final var ni = s[0].length;
        int[] best = null;
        var top = Double.NEGATIVE_INFINITY;
        for (final var size : sizes) {
            final int c = size[0], r = size[1];
            for (var j = 0; j + r <= nj; j++)
                for (var i = 0; i + c <= ni; i++) {
                    double in = 0, ring = 0;
                    var around = 0;
                    var ok = true;
                    for (var y = j - 1; y <= j + r && ok; y++)
                        for (var x = i - 1; x <= i + c; x++) {
                            final var inner = y >= j && y < j + r && x >= i && x < i + c;
                            final var v = y < 0 || x < 0 || y >= nj || x >= ni ? Double.NaN : s[y][x];
                            if (inner && Double.isNaN(v)) { ok = false; break; }
                            if (inner) in += v;
                            else if (!Double.isNaN(v)) { ring += v; around++; }
                        }
                    if (!ok) continue;
                    final var value = in / (c * r) - (around == 0 ? 0 : ring / around);
                    if (value > top) { top = value; best = new int[]{i, j, c, r}; }
                }
        }
        return top > 0 ? best : null;
    }

    /** {@code {i, j, cols, rows}} bounding the largest 4-connected run of periods in the high group. */
    private static int[] largestBlock(double[][] s) {
        final var nj = s.length;
        final var ni = s[0].length;
        final var values = Arrays.stream(s).flatMapToDouble(Arrays::stream).filter(v -> !Double.isNaN(v)).toArray();
        final var cut = split(values)[0];
        final var floor = Double.isNaN(cut) ? Double.NEGATIVE_INFINITY : cut;
        final var seen = new boolean[nj][ni];
        int[] best = null;
        var bestSize = 0;
        for (var j = 0; j < nj; j++)
            for (var i = 0; i < ni; i++) {
                if (seen[j][i] || !(s[j][i] >= floor)) continue;
                int size = 0, minI = i, maxI = i, minJ = j, maxJ = j;
                final var stack = new java.util.ArrayDeque<int[]>();
                stack.push(new int[]{i, j});
                seen[j][i] = true;
                while (!stack.isEmpty()) {
                    final var p = stack.pop();
                    size++;
                    minI = Math.min(minI, p[0]); maxI = Math.max(maxI, p[0]);
                    minJ = Math.min(minJ, p[1]); maxJ = Math.max(maxJ, p[1]);
                    for (final var d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        final var x = p[0] + d[0];
                        final var y = p[1] + d[1];
                        if (x < 0 || y < 0 || x >= ni || y >= nj || seen[y][x] || !(s[y][x] >= floor)) continue;
                        seen[y][x] = true;
                        stack.push(new int[]{x, y});
                    }
                }
                if (size > bestSize) { bestSize = size; best = new int[]{minI, minJ, maxI - minI + 1, maxJ - minJ + 1}; }
            }
        return best;
    }

    /**
     * The value that best separates {@code values} into two groups (Otsu, over 256 bins) and the share of their
     * spread that separation explains: {@code {cut, explained}}. The cut is {@code NaN} when the values do not form
     * two groups (explained below {@link #TWO_GROUPS}).
     */
    static double[] split(double[] values) {
        final var cut = cut(values);
        if (Double.isNaN(cut)) return new double[]{Double.NaN, 0};
        double mean = 0;
        for (final var v : values) mean += v;
        mean /= values.length;
        double total = 0, s0 = 0, s1 = 0;
        int n0 = 0, n1 = 0;
        for (final var v : values) {
            total += (v - mean) * (v - mean);
            if (v < cut) { s0 += v; n0++; } else { s1 += v; n1++; }
        }
        final var explained = total <= 0 || n0 == 0 || n1 == 0 ? 0
                : (n0 * Math.pow(s0 / n0 - mean, 2) + n1 * Math.pow(s1 / n1 - mean, 2)) / total;
        return new double[]{explained >= TWO_GROUPS ? cut : Double.NaN, explained};
    }

    /** The value that best separates {@code values} into two groups (Otsu over 256 bins); {@code NaN} when all are one value. */
    static double cut(double[] values) {
        if (values.length < 2) return Double.NaN;
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (final var v : values) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
        if (hi - lo < 1e-12) return Double.NaN;
        final var bins = 256;
        final var hist = new int[bins];
        for (final var v : values) hist[Math.min(bins - 1, (int) ((v - lo) / (hi - lo) * bins))]++;
        double cut = lo, bestBetween = -1, w0 = 0, sum0 = 0, sumAll = 0;
        for (var b = 0; b < bins; b++) sumAll += hist[b] * (lo + (b + 0.5) * (hi - lo) / bins);
        for (var b = 0; b < bins - 1; b++) {
            w0 += hist[b];
            sum0 += hist[b] * (lo + (b + 0.5) * (hi - lo) / bins);
            final var w1 = values.length - w0;
            if (w0 == 0 || w1 == 0) continue;
            final var m0 = sum0 / w0;
            final var m1 = (sumAll - sum0) / w1;
            final var between = w0 * w1 * (m0 - m1) * (m0 - m1);
            if (between > bestBetween) { bestBetween = between; cut = lo + (b + 1) * (hi - lo) / bins; }
        }
        return cut;
    }

    /**
     * How much of the periods' score spread the best two-group split must explain before it is taken, when no sizes
     * are given: noise round one value splits at about 64% and evenly spread values at 75%, so the bar sits above
     * both; region against background explains well over 90%.
     */
    static final double TWO_GROUPS = 0.78;
}
