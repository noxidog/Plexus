package org.tervel.plexus;

/**
 * How far a read or a slice may extrapolate from what was trained: the highest level at which a pure node may
 * answer for children it never saw. The same setting governs reading (an ASSUMED answer) and generation
 * (ASSUMED grids). Each setting allows everything the one before it does.
 */
public enum Extrapolate {
    /** Not at all: only fully trained paths answer. */
    NONE,
    /** To unseen placements of trained shapes: orientations, poses and corners. */
    PLACEMENT,
    /**
     * Also to unseen symmetry classes under a pure partial-symmetry node, provided the query's mass and joins
     * occur together somewhere under that node: the fold never crosses mass or joins. The default.
     */
    SYMMETRY,
    /**
     * Also to unseen mass, joins and extension values, and to symmetry classes without the mass-and-joins
     * condition: anything that fits a pure node, down to a pure box.
     */
    EVERYTHING;

    /** Whether a pure node splitting on {@code level} may answer for a missing child. */
    boolean allows(Level level) {
        return switch (level) {
            case BOX -> false;                                       // the root never stands in for a box
            case ORIENTATION, POSE, POSITION -> this != NONE;
            case SYMMETRY -> this == SYMMETRY || this == EVERYTHING;
            case MASS, JOINS, EXTENSION -> this == EVERYTHING;
        };
    }
}
