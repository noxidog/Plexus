package org.tervel.plexus.trie;

/**
 * The outcome of one routed walk.
 *
 * <p>{@code depth} is where the descent stopped, which is one of three things: at a <b>pure</b> node (nothing
 * deeper could change the answer, so the remaining coordinates went unevaluated), at the <b>leaf</b>, or at a
 * missing edge ({@code backoff}). {@code evals} counts the coordinates actually computed against
 * {@code fullDepth}, so {@code fullDepth - evals} is what laziness saved; {@code fullDepth} is exact for a
 * fixed chain and an upper bound once extension may deepen a path.
 *
 * <p>{@code seen} means the member's own record answered, which is exact for anything the stored form
 * distinguishes. {@code irreducible} marks the one place an answer degrades to a bare majority: a leaf whose
 * members share a single orbit under both labels, where by proof there is nothing else to consult (§10.4).
 */
public record Resolution(int label, int depth, int fullDepth, int pureAt, long neg, long pos,
                         boolean exception, int evals, boolean backoff,
                         boolean seen, boolean irreducible) {

    /** The answer is the exact resolution (pure-stop or full depth), not a backoff guess. */
    public boolean exact() { return depth >= 0 && !backoff; }

    /** The track was empty — nothing to back off to, the honest reject. */
    public boolean rejected() { return depth < 0; }
}
