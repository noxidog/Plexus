package org.tervel.plexus.trie;

import org.tervel.plexus.diagnostics.Information;
import org.tervel.plexus.diagnostics.Primitive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What to do when an insert lands on a leaf that already holds the opposite label — the <b>third injected
 * seam</b>, alongside the {@link org.tervel.plexus.symmetry.SymmetryGroup} the router folds by and the
 * {@link org.tervel.plexus.residual.ResidualResolver} that owns the residual. Each of the three is a decision
 * the structure deliberately does not make for itself.
 *
 * <p>The policy chooses a <b>column</b>, nothing more. It does not decide whether the contradiction is
 * irreducible: that is settled before it is consulted, by the orbit test, and it is a theorem rather than a
 * policy (members sharing one orbit are provable symmetry-twins, §10.4, so no coordinate could separate them
 * whatever a policy preferred). By the time a policy is asked, the members are known to be separable in
 * principle and the only open question is which coordinate does it.
 *
 * <p>Because a column is read from a stored {@link Member} rather than from a grid, a policy sees exactly the
 * measurements the leaf kept. That is what lets extension work in a store that has discarded its grids.
 */
@FunctionalInterface
public interface ExtensionPolicy {

    /** The vocabulary column to split on, or {@code -1} to decline and leave the contradiction standing. */
    int chooseColumn(long[] counts, Map<Member, long[]> members, List<Primitive> vocabulary);

    /** Never extend: the fixed-chain configuration, where a collision is simply carried. */
    static ExtensionPolicy never() { return (counts, members, vocabulary) -> -1; }

    /**
     * The shipped rule: score every column by <b>information gain</b> over the leaf's members and take the
     * best strictly-positive one. Declining on zero gain matters — a column whose values differ but whose
     * parts are each as mixed as the whole has separated nothing, and splitting on it would deepen the tree
     * while resolving no contradiction.
     *
     * <p>This is the same measure {@link org.tervel.plexus.diagnostics.CollisionDiagnostic} uses to name the
     * next invariant, which is the point: the batch diagnostic advises a human with it, and this applies it
     * at the moment of collision instead.
     */
    static ExtensionPolicy informationGain() {
        return (counts, members, vocabulary) -> {
            var best = -1;
            var bestGain = 1e-9;                              // strictly positive: zero gain is no split
            for (var c = 0; c < vocabulary.size(); c++) {
                final var parts = new HashMap<Long, long[]>();
                for (final var e : members.entrySet()) {
                    final var part = parts.computeIfAbsent(e.getKey().column(c, vocabulary), k -> new long[2]);
                    part[0] += e.getValue()[0];
                    part[1] += e.getValue()[1];
                }
                final var gain = Information.gain(counts, parts.values());
                if (gain > bestGain) { bestGain = gain; best = c; }
            }
            return best;
        };
    }
}
