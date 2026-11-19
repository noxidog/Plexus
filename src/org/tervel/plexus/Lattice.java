package org.tervel.plexus;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * A lattice: the cells, which cells neighbour each other, and the symmetry group they share. Two lattices can
 * share cells and group and still differ in their neighbours ({@link #SQUARE_4} and {@link #SQUARE_8} are both
 * square cells under D4), and the neighbours are part of the invariant: they define the {@code joins} level of
 * the key and every self-extension primitive. So a router is built from a set of lattices once, at
 * construction, and keeps it for its life.
 *
 * <p>A set is legal when it holds exactly one square lattice (rectangles always file on the square lattice, so
 * there must be one, and two definitions of square joins would be ambiguous) and optionally {@link #HEX_6}.
 * Without {@code HEX_6} the hex lattice is never tried and the D6 root stays empty.
 */
public enum Lattice {

    /** Square cells; neighbours share an edge (4). The default square lattice. */
    SQUARE_4(new int[][]{{0, 1}, {1, 0}}),
    /** Square cells; neighbours share an edge or a corner (8), the king's move: minesweeper's neighbourhood. */
    SQUARE_8(new int[][]{{0, 1}, {1, 0}, {1, 1}, {1, -1}}),
    /** Hex cells, reached by shearing a square box (axial {@code (q, r)}); neighbours share an edge (6). */
    HEX_6(new int[][]{{1, 0}, {0, 1}, {1, -1}});

    /** The default set: {@link #SQUARE_4} and {@link #HEX_6}. */
    public static final Set<Lattice> DEFAULT = Set.copyOf(EnumSet.of(SQUARE_4, HEX_6));

    private final int[][] forward;
    private final int[][] all;

    Lattice(int[][] forward) {
        this.forward = forward;
        this.all = new int[forward.length * 2][];
        for (var i = 0; i < forward.length; i++) {
            all[2 * i] = forward[i];
            all[2 * i + 1] = new int[]{-forward[i][0], -forward[i][1]};
        }
    }

    /** Whether this lattice has hex cells. */
    public boolean hex() { return this == HEX_6; }

    /** How many neighbours an interior cell has: 4, 8 or 6. */
    public int neighbours() { return all.length; }

    /** Neighbour offsets {@code {da, db}}, each undirected pair once. */
    int[][] forward() { return forward; }

    /** Every neighbour offset {@code {da, db}}. */
    int[][] all() { return all; }

    /**
     * {@code lattices} as an immutable set, after checking it: exactly one of {@link #SQUARE_4} and
     * {@link #SQUARE_8}, and {@link #HEX_6} at most once.
     *
     * @throws IllegalArgumentException with the reason, when the set is not legal
     */
    public static Set<Lattice> checked(Set<Lattice> lattices) {
        if (lattices == null || lattices.isEmpty())
            throw new IllegalArgumentException("a router needs a square lattice: SQUARE_4 or SQUARE_8");
        if (lattices.stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("null lattice in " + lattices);
        final var squares = lattices.stream().filter(l -> !l.hex()).count();
        if (squares == 0)
            throw new IllegalArgumentException("no square lattice in " + lattices
                    + ": rectangles always file on the square lattice, so add SQUARE_4 or SQUARE_8");
        if (squares > 1)
            throw new IllegalArgumentException("two square lattices in " + lattices
                    + ": square joins must have one meaning, so choose SQUARE_4 or SQUARE_8");
        return Set.copyOf(lattices);
    }

    /** {@link #checked(Set)} over an array. */
    public static Set<Lattice> checked(Lattice... lattices) {
        if (lattices == null) throw new IllegalArgumentException("a router needs a square lattice: SQUARE_4 or SQUARE_8");
        if (Arrays.asList(lattices).contains(null)) throw new IllegalArgumentException("null lattice in " + Arrays.toString(lattices));
        final var set = EnumSet.noneOf(Lattice.class);
        set.addAll(Arrays.asList(lattices));
        if (set.size() != lattices.length) throw new IllegalArgumentException("a lattice is listed twice in " + Arrays.toString(lattices));
        return checked(set);
    }

    /** The square lattice of a legal set. */
    static Lattice square(Set<Lattice> lattices) {
        return lattices.stream().filter(l -> !l.hex()).findFirst().orElseThrow();
    }
}
