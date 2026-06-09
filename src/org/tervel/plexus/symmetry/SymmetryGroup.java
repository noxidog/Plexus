package org.tervel.plexus.symmetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An ordered group of {@link Transform}s over square grids. Everything else is built from
 * <b>stabilizer</b> — which transforms fix a grid — in three forms: the raw self-symmetry, its
 * rotation-canonical form (the routing descriptor), and the "is any rotation a fix?" test (the radial
 * verdict).
 *
 * <p><b>Ordering is a contract.</b> {@link #transforms()} is ordered {@code identity, rotations…,
 * reflections…} and the index of a transform <em>is</em> its bit position in every mask this interface
 * returns ({@link #stabilizer}, {@link #canonicalStabilizer}, {@link #describe}). Consumers (the routing
 * descriptor, the broken/surviving sweep) rely on that alignment, so a derived subgroup
 * ({@link #rotations()}) must preserve relative order.
 *
 * <p><b>Scope: covering groups only.</b> An implementation may exist only for a <em>covering</em>
 * (crystallographic) symmetry order — {@code n ∈ {1, 2, 3, 4, 6}}, the orders whose regular polygon tiles
 * the plane. {@link Dihedral#d4()} (square) and {@link Dihedral#d2()} (rectangle — the transpose-free
 * subgroup) are the realised ones; triangle (D3) and hexagon (D6) are covering-but-unbuilt. Non-covering
 * orders (pentagon and beyond) are aperiodic and provably cannot be a finite group of this form — see
 * {@link Crystallographic} for the split, the {@code φ(n) ≤ 2} criterion, and why Penrose covers fall
 * outside this interface.
 *
 * <p><b>No element represents the null.</b> A group is invertible — every transform has an inverse, so
 * <em>nothing maps to nothing</em>. The empty / zero configuration is the <b>universal fixed point</b>: fixed
 * by every transform precisely because it is the <em>absence</em> of the object the group acts on, and so it
 * lies outside every orbit. It surfaces here only as degenerate cases (the empty grid; an unseen key
 * defaulting to reject in {@link org.tervel.plexus.Plexus#score}; the trivial stabilizer) — it can be
 * <em>labelled</em> but never <em>enacted</em>. This is the framework's own residual: the one thing the
 * symmetry can point at but never hold (PLEXUS_DESIGN §11, <i>What the symmetry holds</i>).
 */
public interface SymmetryGroup {

    /** The transforms, in canonical order; index == bit position in every mask here. */
    List<Transform> transforms();

    /** The rotation subgroup (the transforms with {@link Transform#isRotation()}), order preserved. */
    SymmetryGroup rotations();

    /**
     * The self-symmetry of {@code g}: a bitmask over {@link #transforms()} of those that fix it. This is
     * the routing descriptor's symmetry component and a context's in-key symmetry set. The {@code int[]}-only
     * form assumes a square frame; the {@code (rows, cols)} form carries an explicit rectangular domain.
     */
    default int stabilizer(int[] g) {
        final var s = Transform.side(g);
        return stabilizer(g, s, s);
    }

    /** {@link #stabilizer(int[])} on an explicit {@code rows×cols} frame (a non-square domain). */
    default int stabilizer(int[] g, int rows, int cols) {
        final var ts = transforms();
        var mask = 0;
        for (var k = 0; k < ts.size(); k++) if (ts.get(k).fixes(g, rows, cols)) mask |= 1 << k;
        return mask;
    }

    /** The minimal non-trivial rotation — the generator of the rotation subgroup (rot90 on a square, rot180 on a rectangle). */
    default Transform minimalRotation() { return rotations().transforms().get(1); }

    /**
     * The rotation-canonical self-symmetry of {@code g}: the minimum {@link #stabilizer} over the
     * grid's rotation orbit. Stepping by the {@link #minimalRotation} folds a shape and its rotated
     * copies onto one key, so orientation drops out of the descriptor.
     */
    default int canonicalStabilizer(int[] g) {
        final var s = Transform.side(g);
        return canonicalStabilizer(g, s, s);
    }

    /** {@link #canonicalStabilizer(int[])} on an explicit {@code rows×cols} frame (a non-square domain). */
    default int canonicalStabilizer(int[] g, int rows, int cols) {
        final var step = minimalRotation();
        final var turns = rotations().transforms().size();
        var canonical = Integer.MAX_VALUE;
        var rot = g;
        for (var i = 0; i < turns; i++) {
            canonical = Math.min(canonical, stabilizer(rot, rows, cols));
            rot = step.apply(rot, rows, cols);                          // rotation keeps rows×cols (rot180 / rot90 on square)
        }
        return canonical;
    }

    /** Whether any non-trivial rotation leaves {@code g} unchanged — i.e. {@code g} is rotationally round. */
    default boolean rotationFixes(int[] g) {
        final var s = Transform.side(g);
        return rotationFixes(g, s, s);
    }

    /** {@link #rotationFixes(int[])} on an explicit {@code rows×cols} frame (a non-square domain). */
    default boolean rotationFixes(int[] g, int rows, int cols) {
        final var step = minimalRotation();
        final var turns = rotations().transforms().size();
        var rot = step.apply(g, rows, cols);
        for (var i = 1; i < turns; i++) {
            if (Arrays.equals(rot, g)) return true;
            rot = step.apply(rot, rows, cols);
        }
        return false;
    }

    /** Names of the transforms set in {@code mask}, e.g. {@code "identity, diagMain"}; {@code "(none)"} if empty. */
    default String describe(int mask) {
        final var ts = transforms();
        final var present = new ArrayList<String>();
        for (var k = 0; k < ts.size(); k++) if ((mask & (1 << k)) != 0) present.add(ts.get(k).label());
        return present.isEmpty() ? "(none)" : String.join(", ", present);
    }
}
