package org.tervel.plexus.residual;

import org.tervel.plexus.Plexus.Context;

/**
 * The <b>deterministic</b> residual resolver — the default, and the honest floor. Within a routed context it
 * returns the context's <b>majority label</b>, flipped <em>iff</em> the grid is an exact memorised
 * exception (a symmetry-twin whose label contradicts the converged majority).
 *
 * <p>It is exact on every seen contradiction and provably never over-flips — a genuine twin is a different
 * grid, never in the store. The price is that it does <b>not generalise</b>: an unseen twin falls through to
 * the majority. That is the deterministic boundary {@link ResidualResolver} marks — to cross it (predict the
 * residual rather than memorise it) you swap in a brute-force or learned resolver here.
 *
 * <p>Read outward, the same residual is <b>any scalar with no structural home</b>: the price gap between two
 * structural twins — value that is not <em>in</em> the object — is "hype", a residual of exactly this kind
 * (PLEXUS_DESIGN §11, <i>What the symmetry holds</i>).
 */
public final class ExactExceptions implements ResidualResolver {

    @Override
    public int resolve(Context context, int[] grid) {
        return context.isException(grid) ? 1 - context.answer() : context.answer();
    }
}
