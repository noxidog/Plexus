package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.symmetry.Shape;
import org.tervel.plexus.symmetry.SymmetryGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factor a shape into <b>atoms</b> — the decomposition direction of §9, run as an {@link Operation} (a
 * sibling of {@link Searcher} and {@link Fiber}). It cuts the shape's cell-graph into strands at its
 * junctions and folds congruent strands together, so a shape is read as a small dictionary of atom shapes
 * plus the symmetry that replays the copies.
 *
 * <h2>The cut-law is a parameter (§3, §9)</h2>
 * Whether a <b>bend</b> ends a strand is the one knob between the two decompositions discussed in §3:
 * <ul>
 *   <li>{@link CutLaw#STRAIGHT} — a strand must stay straight, so a corner ends it. Atoms are <b>bars</b>
 *       and dots. The T → a 3-bar plus a dot.</li>
 *   <li>{@link CutLaw#BENT} — a strand may turn (it prefers a turn at a junction), so bends are interior.
 *       Atoms are <b>Ls / paths</b> and dots. The T → an L plus a dot.</li>
 * </ul>
 *
 * <h2>The stabilizer does the folding (§9)</h2>
 * The atoms are grouped into a <b>dictionary</b> of distinct canonical {@link Shape}s with multiplicities:
 * congruent strands (e.g. the two arms of a T, the four arms of a plus) collapse to one entry, and each
 * entry carries its {@code isotropy}. That collapse is exactly the orbit fold of §9 — the more symmetry the
 * shape keeps, the fewer distinct atoms — so the dictionary size is the compression the stabilizer buys.
 */
public final class Decompose implements Operation<Decompose.Decomposition> {

    /** How a strand may grow: straight only (bars), or through bends (Ls / paths). */
    public enum CutLaw { STRAIGHT, BENT }

    /** What a strand turned out to be, by length and bend count. */
    public enum Kind { DOT, BAR, ELL, PATH }

    /** One atom: the original cells it covers, its kind, and its canonical frame-free {@link Shape}. */
    public record Atom(List<Integer> cells, Kind kind, Shape shape) { }

    /** The result: the atoms, a dictionary (distinct atom shapes → count), and the shape's stabilizer. */
    public record Decomposition(List<Atom> atoms, Map<String, Integer> dictionary, String stabilizer,
                                CutLaw law) {

        /** A human rendering: the cut-law, the atoms, the folded dictionary, and the stabilizer note. */
        public String render() {
            final var sb = new StringBuilder();
            sb.append("decompose [").append(law).append("]  (cut at junctions; ")
              .append(law == CutLaw.STRAIGHT ? "corners end a strand -> bars" : "bends kept -> Ls").append(")\n");
            sb.append("  shape stabilizer: [").append(stabilizer).append("]   (|Stab| sets the fold)\n");
            sb.append("  atoms (").append(atoms.size()).append("):\n");
            for (var i = 0; i < atoms.size(); i++) {
                final var a = atoms.get(i);
                sb.append(String.format("    #%d  %-4s  cells=%s  %-10s isotropy %s%n",
                        i + 1, a.kind(), a.cells(), ascii(a.shape()), a.shape().isotropyFraction()));
            }
            sb.append("  dictionary (distinct atoms up to symmetry):\n");
            dictionary.forEach((shape, n) -> sb.append(String.format("    %dx  %s%n", n, shape)));
            return sb.toString();
        }
    }

    private final int[] grid;
    private final CutLaw law;

    public Decompose(int[] grid, CutLaw law) {
        this.grid = grid.clone();
        this.law = law;
    }

    @Override
    public Decomposition apply(Plexus plexus) {
        final var side = (int) Math.round(Math.sqrt(grid.length));
        final var group = plexus.group();
        final var visited = new boolean[grid.length];
        final var atoms = new ArrayList<Atom>();

        for (var start = pickStart(side, visited); start >= 0; start = pickStart(side, visited))
            atoms.add(makeAtom(walk(side, visited, start), side, group));

        // Fold congruent atoms into a dictionary of distinct canonical shapes — the §9 orbit collapse.
        final var dictionary = new LinkedHashMap<String, Integer>();
        for (final var a : atoms) dictionary.merge(ascii(a.shape()), 1, Integer::sum);

        final var centered = Plexus.center(grid);
        final var stab = centered == null ? "(empty)" : group.describe(group.stabilizer(centered));
        return new Decomposition(List.copyOf(atoms), dictionary, stab, law);
    }

    // ---- strand walk -------------------------------------------------------------------------

    /** Next start: the unvisited cell with the fewest unvisited neighbours (an endpoint first), by index. */
    private int pickStart(int side, boolean[] visited) {
        var best = -1;
        var bestDegree = Integer.MAX_VALUE;
        for (var i = 0; i < grid.length; i++) {
            if (grid[i] != 1 || visited[i]) continue;
            final var d = neighbours(i, side, visited).size();
            if (d < bestDegree) { bestDegree = d; best = i; }
        }
        return best;
    }

    /** Walk one maximal strand from {@code start} under the cut-law, marking cells visited as it goes. */
    private List<Integer> walk(int side, boolean[] visited, int start) {
        final var path = new ArrayList<Integer>();
        var cur = start;
        visited[start] = true;
        path.add(start);
        var have = false;          // whether we have an incoming direction yet
        var dr = 0;
        var dc = 0;
        while (true) {
            final var next = choose(cur, side, neighbours(cur, side, visited), have, dr, dc);
            if (next == null) break;
            dr = next / side - cur / side;
            dc = next % side - cur % side;
            have = true;
            visited[next] = true;
            path.add(next);
            cur = next;
        }
        return path;
    }

    /** Pick the continuation among {@code nbrs} per the cut-law; {@code null} ends the strand. */
    private Integer choose(int cur, int side, List<Integer> nbrs, boolean have, int dr, int dc) {
        if (nbrs.isEmpty()) return null;
        if (!have) return nbrs.get(0);                       // first step: smallest-index neighbour
        Integer straight = null;
        final var turns = new ArrayList<Integer>();
        for (final var n : nbrs) {
            if (n / side - cur / side == dr && n % side - cur % side == dc) straight = n;
            else turns.add(n);
        }
        return switch (law) {
            case STRAIGHT -> straight;                       // null at a corner/junction-turn ⇒ strand ends
            case BENT -> turns.isEmpty() ? straight : turns.get(0);   // prefer a turn, keep the strand going
        };
    }

    /** Unvisited filled orthogonal neighbours of {@code i}, in ascending index order. */
    private List<Integer> neighbours(int i, int side, boolean[] visited) {
        final var out = new ArrayList<Integer>();
        final var r = i / side;
        final var c = i % side;
        if (r > 0)        add(out, i - side, visited);
        if (c > 0)        add(out, i - 1, visited);
        if (c < side - 1) add(out, i + 1, visited);
        if (r < side - 1) add(out, i + side, visited);
        return out;
    }

    private void add(List<Integer> out, int j, boolean[] visited) {
        if (grid[j] == 1 && !visited[j]) out.add(j);
    }

    // ---- atom shaping ------------------------------------------------------------------------

    /** Turn a walked path into an {@link Atom}: classify it by bends and read its canonical {@link Shape}. */
    private Atom makeAtom(List<Integer> path, int side, SymmetryGroup group) {
        var bends = 0;
        for (var k = 2; k < path.size(); k++) {
            final int p0 = path.get(k - 2), p1 = path.get(k - 1), p2 = path.get(k);
            if ((p1 / side - p0 / side) != (p2 / side - p1 / side)
                    || (p1 % side - p0 % side) != (p2 % side - p1 % side)) bends++;
        }
        final var kind = path.size() == 1 ? Kind.DOT : bends == 0 ? Kind.BAR : bends == 1 ? Kind.ELL : Kind.PATH;
        final var g = new int[grid.length];
        for (final var idx : path) g[idx] = 1;
        return new Atom(List.copyOf(path), kind, Shape.of(g, group));
    }

    /** Compact {@code "row/row"} rendering of a canonical shape's pattern (e.g. {@code "xx/x."}). */
    private static String ascii(Shape s) {
        final var sb = new StringBuilder();
        for (var r = 0; r < s.height(); r++) {
            for (var c = 0; c < s.width(); c++) sb.append(s.cells()[r * s.width() + c] == 1 ? 'x' : '.');
            if (r < s.height() - 1) sb.append('/');
        }
        return sb.isEmpty() ? "." : sb.toString();
    }
}
