package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.Plexus.Verdict;
import org.tervel.plexus.symmetry.Transform;

import java.util.Iterator;

/**
 * The descriptor run backwards in the <b>symmetry direction</b>. Configured with an {@code input} grid and
 * a {@code target} value, applied to a {@link Plexus} it sweeps the Plexus's symmetry group and yields the
 * transformations whose image of the input the Plexus classifies as {@code target}. The group is the
 * "wiggle room": you hold the input fixed, wiggle it by each symmetry, and report where it lands on the
 * target. This reads out the symmetry structure of a classification directly:
 * <ul>
 *   <li>{@link Verdict#POSITIVE} on an exception yields its <b>broken</b> symmetries (the transforms that
 *       turn it into a positively-classified grid);</li>
 *   <li>{@link Verdict#NEGATIVE} yields its <b>surviving</b> symmetries (the transforms under which it
 *       stays the exception);</li>
 *   <li>{@link Verdict#UNSEEN} yields the transforms that carry it <b>off the trained map</b> entirely.</li>
 * </ul>
 *
 * <p>The sweep matches on {@link Plexus#verdict}, not {@link Plexus#score}. Under {@code score} the reject
 * and the negative class share the value {@code 0}, so a {@code target = 0} sweep silently counted
 * never-explored transforms among the <b>surviving</b> symmetries — reporting absence of evidence as
 * evidence of the label. The three verdicts are disjoint and exhaust the group, so the broken / surviving /
 * unseen sets now partition the transforms exactly.
 *
 * <h2>Why this is ever non-trivial</h2>
 * If the chain is orbit-constant (the invariance contract) and no exception is involved, every image routes
 * to the same context as the input and the sweep returns all of the group or none of it. It resolves to
 * something in between exactly where orbit-constancy is <em>deliberately</em> broken — the exception store
 * (which matches raw grids, so an image can be a memorised exception when the input is not) and the
 * radial-only {@code Position} axis. So the Searcher is the instrument that measures <b>where the quotient
 * leaks</b>: it reads out the pose-dependence routing normally folds away.
 *
 * <p>The other inverse direction — enumerating a node's grids — is the descriptor-fiber, in {@link Fiber}.
 */
public final class Searcher implements Operation<Iterator<Transform>> {

    private final int[] input;
    private final Verdict target;

    public Searcher(int[] input, Verdict target) {
        this.input = input.clone();
        this.target = target;
    }

    /**
     * Binary convenience for callers holding a 0/1 label: {@code 1} means {@link Verdict#POSITIVE},
     * {@code 0} means {@link Verdict#NEGATIVE} — a <em>judged</em> negative. {@link Verdict#UNSEEN} is
     * deliberately unreachable this way; ask for it explicitly.
     */
    public Searcher(int[] input, int target) {
        this(input, target == 1 ? Verdict.POSITIVE : Verdict.NEGATIVE);
    }

    @Override
    public Iterator<Transform> apply(Plexus plexus) {
        return plexus.group().transforms().stream()
                .filter(t -> plexus.verdict(t.apply(input)) == target)
                .iterator();
    }
}
