package org.tervel.plexus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * What the planes of a {@link Grid} are made of. Each cell of a grid carries one <b>label</b>; a
 * <b>plane</b> is a named set of cells, and a plane name is resolved, in order, as:
 *
 * <ol>
 *   <li>a name in {@link #everywhere}: every cell of the grid;</li>
 *   <li>a plane a stage {@linkplain Grid#mark marked} the cell in;</li>
 *   <li>a label: the cells carrying it;</li>
 *   <li>a class: the cells carrying any of its labels.</li>
 * </ol>
 *
 * <p>Off the grid only the names in {@link #outside} hold, so a template cell may lie past the edge
 * exactly where the vocabulary says the edge looks like that plane.
 *
 * <p>Labels come in two kinds. Most are <b>read</b>: the oracle observes them. A <b>state</b> is
 * <b>written</b>: a stage concludes it. The two never mix — the oracle may not answer with a state and a
 * stage may write nothing else — so a conclusion can never pass for an observation.
 *
 * @param classes    class name → the labels it groups
 * @param values     label → the integer it carries
 * @param outside    plane names that hold for a cell off the grid
 * @param everywhere plane names that hold for every cell on the grid, whatever its label
 * @param states     the labels a stage may write and the oracle may not answer
 */
public record Vocabulary(Map<String, Set<String>> classes, Map<String, Integer> values, Set<String> outside,
                         Set<String> everywhere, Set<String> states) {

    public Vocabulary {
        final var copy = new LinkedHashMap<String, Set<String>>();
        classes.forEach((k, v) -> copy.put(k, Set.copyOf(v)));
        classes = Map.copyOf(copy);
        values = Map.copyOf(values);
        outside = Set.copyOf(outside);
        everywhere = Set.copyOf(everywhere);
        states = Set.copyOf(states);
    }

    /** Whether a cell carrying {@code label} on the grid is in {@code plane} by label or class. */
    public boolean labels(String plane, String label) {
        if (label == null) return false;
        if (label.equals(plane)) return true;
        final var members = classes.get(plane);
        return members != null && members.contains(label);
    }
}
