package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.symmetry.Transform;

import java.util.Iterator;

/**
 * The descriptor run backwards in the <b>symmetry direction</b>. Configured with an {@code input} grid and
 * a {@code target} value, applied to a {@link Plexus} it sweeps the Plexus's symmetry group and yields the
 * transformations whose image of the input the Plexus classifies as {@code target}. The group is the
 * "wiggle room": you hold the input fixed, wiggle it by each symmetry, and report where it lands on the
 * target. This reads out the symmetry structure of a classification directly:
 * <ul>
 *   <li>{@code target = 1} on an exception yields its <b>broken</b> symmetries (the transforms that turn
 *       it into a positively-classified grid);</li>
 *   <li>{@code target = 0} yields its <b>surviving</b> symmetries (the transforms under which it stays
 *       the exception).</li>
 * </ul>
 *
 * <p>The other inverse direction — enumerating a node's grids — is the descriptor-fiber, in {@link Fiber}.
 */
public final class Searcher implements Operation<Iterator<Transform>> {

    private final int[] input;
    private final int target;

    public Searcher(int[] input, int target) {
        this.input = input.clone();
        this.target = target;
    }

    @Override
    public Iterator<Transform> apply(Plexus plexus) {
        return plexus.group().transforms().stream()
                .filter(t -> plexus.score(t.apply(input)) == target)
                .iterator();
    }
}
