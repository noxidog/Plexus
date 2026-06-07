package org.tervel.plexus.utility;

import java.util.List;

/**
 * <b>Optimal stopping under a {@link Utility} and a {@link Strategy}</b> — the secretary / <b>Sultan's dowry
 * problem</b>: outcomes arrive one at a time, each scored by the utility, and you must <em>commit or discard
 * on the spot</em> with no recall. This class is the harness; the {@link Strategy} is the rule — the harness
 * walks the sequence, tracks the running benchmark, and asks the strategy {@code commit?} at each step.
 *
 * <p>The shipped default is {@link Strategy#NEUTRAL}, the Sultan's-dowry {@code 1/e} rule: observe {@code
 * ⌊n/e⌋} outcomes to set a benchmark, then commit to the first later outcome that beats them, landing on the
 * global best with probability {@code → 1/e ≈ 0.368}. Swap in {@link Strategy#SAFE} / {@link Strategy#RISKY}
 * (or any custom strategy) to slide along the safe↔risky personality axis. The dowry's own objective — marry
 * the best, or go home — is itself a utility, exposed as {@link #reward()}.
 *
 * @param <T> the kind of outcome being courted
 */
public final class OptimalStop<T> {

    /** The outcome of a stopping run: who was chosen, its worth, whether it was the global best, and where. */
    public record Choice<T>(T chosen, double value, boolean optimal, int index, int total) { }

    private final Utility<T> utility;
    private final Strategy strategy;

    /** Court under the Sultan's-dowry ({@link Strategy#NEUTRAL}) strategy — the shipped default. */
    public OptimalStop(Utility<T> utility) { this(utility, Strategy.NEUTRAL); }

    /** Court under a chosen strategy ({@link Strategy#SAFE} / {@code NEUTRAL} / {@code RISKY}, or custom). */
    public OptimalStop(Utility<T> utility, Strategy strategy) {
        this.utility = utility;
        this.strategy = strategy;
    }

    /** Convenience: court under a threshold strategy at the given observation fraction. */
    public OptimalStop(Utility<T> utility, double ratio) {
        this(utility, Strategy.threshold("ratio=" + ratio, ratio));
    }

    /**
     * Run the strategy over a finite arrival order: at each outcome, ask the strategy to commit (given its
     * position, the running benchmark, and the current worth), falling through to the last outcome if the
     * strategy never commits. Returns {@code null} for an empty sequence.
     */
    public Choice<T> apply(List<T> candidates) {
        final var n = candidates.size();
        if (n == 0) return null;

        final var score = new double[n];                       // score once; utilities may be expensive (Fiber)
        var best = Double.NEGATIVE_INFINITY;
        for (var i = 0; i < n; i++) { score[i] = utility.applyAsDouble(candidates.get(i)); best = Math.max(best, score[i]); }

        var bench = Double.NEGATIVE_INFINITY;                  // best seen strictly before the current outcome
        for (var i = 0; i < n; i++) {
            if (strategy.commit(i, n, score[i], bench)) return new Choice<>(candidates.get(i), score[i], score[i] == best, i, n);
            bench = Math.max(bench, score[i]);
        }
        return new Choice<>(candidates.get(n - 1), score[n - 1], score[n - 1] == best, n - 1, n);   // forced last pick
    }

    /** The Sultan's-dowry objective itself, as a utility: {@code 1.0} iff the run married the global best. */
    public static <T> Utility<Choice<T>> reward() {
        return Utility.of("dowry-win", c -> c != null && c.optimal() ? 1.0 : 0.0);
    }
}
