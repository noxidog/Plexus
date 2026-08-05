package org.tervel.plexus.trie;

import java.util.function.ToLongFunction;

/**
 * The coordinate a node splits its children on. Carrying it <b>on the node</b> rather than indexing a global
 * chain by depth is what makes the trie self-describing, and that is what makes extension <b>local</b>: one
 * node can deepen without any other node knowing.
 *
 * <p>{@code column >= 0} marks an axis that is also readable from a stored {@link Member} — a discovered
 * vocabulary coordinate. Structural axes (the stabilizer edge, the sector's chain) carry {@code -1}: they are
 * evaluated on the grid during descent and never need to be read back from a member, because a split only
 * ever happens below them.
 */
public record Axis(String name, ToLongFunction<int[]> onGrid, int column) {

    /** A structural axis: evaluated on the grid, not stored on members. */
    static Axis structural(String name, ToLongFunction<int[]> onGrid) { return new Axis(name, onGrid, -1); }
}
