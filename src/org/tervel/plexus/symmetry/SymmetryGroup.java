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
 */
public interface SymmetryGroup {

    /** The transforms, in canonical order; index == bit position in every mask here. */
    List<Transform> transforms();

    /** The rotation subgroup (the transforms with {@link Transform#isRotation()}), order preserved. */
    SymmetryGroup rotations();

    /**
     * The self-symmetry of {@code g}: a bitmask over {@link #transforms()} of those that fix it. This is
     * the routing descriptor's symmetry component and a context's in-key symmetry set.
     */
    default int stabilizer(int[] g) {
        final var ts = transforms();
        var mask = 0;
        for (var k = 0; k < ts.size(); k++) if (ts.get(k).fixes(g)) mask |= 1 << k;
        return mask;
    }

    /** The minimal non-trivial rotation — the generator of the rotation subgroup (rot90 in 2D). */
    default Transform minimalRotation() { return rotations().transforms().get(1); }

    /**
     * The rotation-canonical self-symmetry of {@code g}: the minimum {@link #stabilizer} over the
     * grid's rotation orbit. Stepping by the {@link #minimalRotation} folds a shape and its rotated
     * copies onto one key, so orientation drops out of the descriptor.
     */
    default int canonicalStabilizer(int[] g) {
        final var step = minimalRotation();
        final var turns = rotations().transforms().size();
        var canonical = Integer.MAX_VALUE;
        var rot = g;
        for (var i = 0; i < turns; i++) {
            canonical = Math.min(canonical, stabilizer(rot));
            rot = step.apply(rot);
        }
        return canonical;
    }

    /** Whether any non-trivial rotation leaves {@code g} unchanged — i.e. {@code g} is rotationally round. */
    default boolean rotationFixes(int[] g) {
        final var step = minimalRotation();
        final var turns = rotations().transforms().size();
        var rot = step.apply(g);
        for (var i = 1; i < turns; i++) {
            if (Arrays.equals(rot, g)) return true;
            rot = step.apply(rot);
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
