package org.tervel.plexus;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * A candidate shape coordinate for self-extension: a property of the shape's cell graph on the lattice that
 * described it ({@link Lattice}: 4 or 8 neighbours square, 6 hex), the same adjacency that defines its joins.
 * Each depends only on which cells touch, so it is unchanged by every turn and mirror of that lattice and is a
 * legal coordinate on either trie. Nothing here refers to a direction, which is what keeps the same battery
 * valid under D4 and D6.
 */
public record Primitive(String name, ToIntFunction<int[]> fromDegrees, boolean usesComponents) {

    /** The battery, in the order ties are broken. */
    public static final List<Primitive> BATTERY = List.of(
            new Primitive("max-degree", d -> max(d), false),
            new Primitive("branch-points", d -> count(d, 3), false),
            new Primitive("end-points", d -> exactly(d, 1), false),
            new Primitive("pieces", d -> 0, true),
            new Primitive("loops", d -> 0, true));

    /** The value on {@code shape}, with {@code lattice}'s neighbours. */
    int value(CellSet shape, Lattice lattice) {
        final var all = lattice.all();
        final var degrees = new int[shape.size()];
        for (var i = 0; i < shape.size(); i++)
            for (final var d : all) if (shape.contains(shape.a(i) + d[0], shape.b(i) + d[1])) degrees[i]++;
        if (!usesComponents) return fromDegrees.applyAsInt(degrees);
        final var pieces = pieces(shape, all);
        if (name.equals("pieces")) return pieces;
        final var edges = shape.edges(lattice.forward());
        return edges - shape.size() + pieces;                          // independent loops (cycle rank)
    }


    private static int max(int[] d) { var m = 0; for (final var x : d) m = Math.max(m, x); return m; }
    private static int count(int[] d, int atLeast) { var n = 0; for (final var x : d) if (x >= atLeast) n++; return n; }
    private static int exactly(int[] d, int v) { var n = 0; for (final var x : d) if (x == v) n++; return n; }

    private static int pieces(CellSet s, int[][] all) {
        final var seen = new boolean[s.size()];
        var pieces = 0;
        for (var start = 0; start < s.size(); start++) {
            if (seen[start]) continue;
            pieces++;
            final var stack = new ArrayDeque<Integer>();
            stack.push(start);
            seen[start] = true;
            while (!stack.isEmpty()) {
                final int i = stack.pop();
                for (final var d : all) {
                    final int na = s.a(i) + d[0], nb = s.b(i) + d[1];
                    for (var j = 0; j < s.size(); j++)
                        if (!seen[j] && s.a(j) == na && s.b(j) == nb) { seen[j] = true; stack.push(j); }
                }
            }
        }
        return pieces;
    }
}
