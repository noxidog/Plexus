package org.tervel.plexus.trie;

import org.tervel.plexus.diagnostics.Primitive;
import org.tervel.plexus.symmetry.Shape;

import java.util.Arrays;
import java.util.List;

/**
 * One member of a leaf, in whichever form the trie was told to remember (see {@link Trie.LeafMemory}). Both
 * forms answer the only two questions the structure asks of a member:
 *
 * <ul>
 *   <li>{@link #column} — its value on a vocabulary coordinate, which is what a split reads to redistribute
 *       members into children. Because a {@link Descriptor} answers it from stored measurements, a split
 *       never needs the grid back.</li>
 *   <li>{@link #orbit} — its canonical {@link Shape}. Equal orbits under equal columns is the provable-twin
 *       condition (§10.4): nothing can separate them, so the contradiction is irreducible.</li>
 * </ul>
 *
 * <p>Identity is what distinguishes the two forms. A {@link Grid} is equal only to the same cells, so every
 * distinct grid is its own row. A {@link Descriptor} is equal to anything measuring the same, so members
 * merge — which is why a twin leaf collapses to a single row carrying both labels, and why that collapse is
 * itself the twin verdict rather than a separate test.
 */
public sealed interface Member {

    /** The canonical orbit; may be {@code null} when extension is disabled (see {@link Trie#member}). */
    Shape orbit();

    /** This member's value on vocabulary column {@code c}. */
    long column(int c, List<Primitive> vocabulary);

    /** A member kept verbatim: exact recall of every distinct grid, at the cost of holding them all. */
    record Grid(int[] cells, Shape orbit) implements Member {
        @Override public long column(int c, List<Primitive> vocabulary) {
            return vocabulary.get(c).quantized(cells);
        }
        @Override public boolean equals(Object o) {
            return o instanceof Grid g && Arrays.equals(cells, g.cells);
        }
        @Override public int hashCode() { return Arrays.hashCode(cells); }
    }

    /** A member kept as measurements only: every vocabulary column plus the orbit, and no grid. */
    record Descriptor(long[] axes, Shape orbit) implements Member {
        @Override public long column(int c, List<Primitive> vocabulary) { return axes[c]; }
        @Override public boolean equals(Object o) {
            return o instanceof Descriptor d && Arrays.equals(axes, d.axes) && orbit.equals(d.orbit);
        }
        @Override public int hashCode() { return Arrays.hashCode(axes) * 31 + orbit.hashCode(); }
    }
}
