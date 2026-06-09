package org.tervel.plexus.residual;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.Plexus.Context;

import java.util.List;

/**
 * The <b>residual boundary</b> — the exact theoretical line where structural routing ends and something
 * else begins.
 *
 * <p>Up to the twin floor (§10.4) the descriptor is <b>exact and deterministic</b>: symmetry plus the
 * invariant chain route every grid to a context by pure structure, no weights, no statistics. But two
 * symmetry-twins share every group-derived quantity by proof, so the group algebra has <em>nothing left to
 * say</em> about which one carries which label. That gap is the residual — the incompressible signal that
 * lives outside the symmetry algebra (§10.4, §10 "entropy = log|fiber|"). Resolving it is no longer routing;
 * it is whatever you are willing to bring to bear: an exact lookup, a brute-force search, or a learned net.
 *
 * <p>This interface is that injection seam. A {@code Plexus} routes a grid to its {@link Context} — the most
 * structure can do — and then hands the final, intra-context decision to a {@code ResidualResolver}. The
 * routing stays principled and deterministic; the resolver is the swappable policy for the part that isn't.
 *
 * <h2>The spectrum of resolvers</h2>
 * <ul>
 *   <li><b>{@link ExactExceptions} (default, deterministic).</b> Majority label, flipped iff the grid is a
 *       memorised exception. Correct on every <em>seen</em> contradiction, never over-flips — but does not
 *       generalise: an <em>unseen</em> twin is answered by the majority. This is honest memorisation, the
 *       deterministic floor.</li>
 *   <li><b>Brute force / a learned net.</b> The same seam admits a resolver that <em>predicts</em>
 *       the residual instead of memorising it — fit a model on the in-context contradictions and have it
 *       generalise to unseen twins, trading exactness for coverage. Nothing structural changes; only this
 *       leaf decision is delegated to a function approximator.</li>
 * </ul>
 *
 * The contract: a resolver only ever decides <em>within one already-routed context</em>. It cannot move a
 * grid to a different context, cannot see other contexts, and is reached only after structure is exhausted —
 * so it can never corrupt the deterministic routing, only fill the residual the routing provably cannot.
 */
public interface ResidualResolver {

    /** Decide the label of {@code grid}, which has already been routed to {@code context}. */
    int resolve(Context context, int[] grid);

    /**
     * Train-time hook, called once after the contexts and their exception stores have converged (the seal
     * pass of {@link Plexus#fit}). The deterministic default needs nothing — the {@link Context} already
     * holds its exact exceptions — so it is a no-op. A learned or brute-force resolver overrides this to fit
     * itself on the sealed residual (the in-context contradictions) before any scoring happens.
     */
    default void seal(Plexus plexus, List<int[]> grids, List<Integer> labels) { }
}
