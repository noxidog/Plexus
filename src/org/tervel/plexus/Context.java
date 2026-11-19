package org.tervel.plexus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The node that controls an answer: the full path to it, its counts and its majority. For a trained grid it is
 * the grid's own leaf; for an ASSUMED one it is the pure node the answer came from.
 *
 * @param root      which trie
 * @param path      the edges from the root down to this node
 * @param negatives training examples labelled 0 that reached this node
 * @param positives training examples labelled 1 that reached this node
 * @param answer    the node's majority label
 * @param depth     edges from the root (an ASSUMED answer from a deeper node is a safer bet)
 * @param boundary  whether this node's parent is mixed (an ASSUMED answer here sits next to disagreement)
 */
public record Context(Root root, List<Step> path, long negatives, long positives, int answer, int depth,
                      boolean boundary) {

    /** One edge: the coordinate a node split on and the value taken. */
    public record Step(Level level, String coordinate, long value, String text) {
        @Override public String toString() { return coordinate + "=" + text; }
    }

    /** Whether every training example under this node carried one label. */
    public boolean pure() { return negatives == 0 || positives == 0; }

    @Override public String toString() {
        return root + " " + path.stream().map(Step::toString).collect(Collectors.joining(" → "))
                + "  [neg " + negatives + ", pos " + positives + "]";
    }
}
