package org.tervel.plexus.invariants;

import org.tervel.plexus.symmetry.Transform;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * A <b>group invariant</b>: a grid coordinate that is unchanged by every routing {@link Transform}, so it
 * is constant on each symmetry orbit. After the symmetry stage quotients a shape to its orbit, each
 * invariant contributes an orthogonal coordinate that further splits the orbit without undoing the
 * canonicalization. That invariance is the contract every implementor owes — break it and orbit-mates
 * route to different contexts, shattering the class. (Position is the exception that proves the rule: it is
 * <em>not</em> rotation-invariant, so it {@link #applies} only to radial shapes, which have no orientation
 * and for which position is well-defined again.)
 *
 * <p>An invariant <em>is</em> a coordinate map {@code grid -> int}, hence a {@link ToIntFunction}{@code
 * <int[]>}; {@link #applyAsInt} is that coordinate. The values are assembled into a composite key (a
 * tuple), so there is no fixed bit budget and no layout to assign — each invariant just yields its
 * coordinate.
 */
public interface Invariant extends ToIntFunction<int[]> {

    /** The coordinate extracted from a grid (the {@link ToIntFunction} of the invariant). */
    @Override
    int applyAsInt(int[] grid);

    /** Whether this invariant refines radial (round) shapes, oriented shapes, or both. */
    boolean applies(boolean radial);

    /** Short label for reports, e.g. {@code "mass"}. */
    String name();

    /**
     * The <b>structural scalar components</b> this coordinate is built from — one entry per genuinely
     * independent quantity, <em>unpacked</em>. A plain invariant is its own single component; a composite
     * like {@link Signature} (which packs {@code V} and {@code E} into one injective column purely for
     * keying) overrides this to expose the two parts separately.
     *
     * <p>This is the seam the <b>thread-volume = determinant</b> identity needs: the Jacobian/Gram volume
     * differentiates these components, never the packed value. A finite difference of the {@code V·W + E}
     * packing mixes the incommensurable {@code W}-unit (a cell) and {@code 1}-unit (an edge) onto one axis,
     * so its gradient is meaningless; differentiating {@code V} and {@code E} as separate rows is the honest
     * Jacobian. {@code Map}-key equality reads the packed column; the determinant reads these components.
     */
    default List<ToIntFunction<int[]>> components() { return List.of(this); }

    /**
     * The invariance contract, made executable. The coordinate must be unchanged by a routing
     * {@link Transform} — {@code applyAsInt(t.apply(g)) == applyAsInt(g)} — or it would route orbit-mates
     * to different contexts and shatter the class. This is the relation to the symmetry: an invariant
     * <em>is</em> what a {@code Transform} leaves fixed. (Position satisfies it only for the transforms
     * that fix a radial shape, which is exactly why it {@link #applies} to radial shapes alone.)
     */
    default boolean invariantUnder(Transform t, int[] grid) {
        return applyAsInt(t.apply(grid)) == applyAsInt(grid);
    }

    /** Side length of a square grid. */
    static int side(int[] g) { return (int) Math.round(Math.sqrt(g.length)); }

    /** Bounding box of the active cells as {@code {height, width}}; {@code {0,0}} if the grid is empty. */
    static int[] box(int[] g) {
        final var side = side(g);
        var minR = side; var maxR = -1; var minC = side; var maxC = -1;
        for (var i = 0; i < g.length; i++) if (g[i] == 1) {
            final int r = i / side, c = i % side;
            minR = Math.min(minR, r); maxR = Math.max(maxR, r);
            minC = Math.min(minC, c); maxC = Math.max(maxC, c);
        }
        return maxR < 0 ? new int[]{0, 0} : new int[]{maxR - minR + 1, maxC - minC + 1};
    }
}
