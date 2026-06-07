package org.tervel.plexus.utility;

/**
 * A <b>stopping strategy</b>: the explore-vs-commit stance taken while courting a sequence with no recall.
 * It is the <em>personality dial</em> of optimal stopping (§ the safe↔risky axis) — at each arriving outcome
 * it decides, from the run so far, whether to commit or pass. Following the codebase idiom (each concept
 * <em>is</em> a JDK functional interface), a {@code Strategy} is a single decision method.
 *
 * <p>The shipped trio anchors the safe↔risky axis, with the <b>Sultan's dowry as the calibrated
 * {@link #NEUTRAL}</b> between the two poles:
 *
 * <ul>
 *   <li>{@link #SAFE} — commit to the first outcome (bird in hand). Never waits, never ends empty-handed,
 *       always settles low. The risk-averse pole: P(best) = 1/n.</li>
 *   <li>{@link #NEUTRAL} — the <b>Sultan's dowry / secretary {@code 1/e} rule</b>: observe {@code ⌊n/e⌋},
 *       then take the first to beat them. Maximises the chance of landing on the best (→ {@code 1/e}).</li>
 *   <li>{@link #RISKY} — observe everything, hold out for the very best. High variance; risks overshooting
 *       into the forced last pick. The risk-seeking pole: P(best) = 1/n.</li>
 * </ul>
 *
 * Any intermediate lean is available via {@link #threshold(String, double)}, and a fully custom rule (e.g. a
 * volume-aware declining threshold) by implementing {@link #commit} directly.
 */
@FunctionalInterface
public interface Strategy {

    /** The Sultan's-dowry observation fraction and asymptotic win rate: {@code 1/e ≈ 0.368}. */
    double SECRETARY_RATIO = 1.0 / Math.E;

    /**
     * Commit to the outcome now arriving, or pass and keep observing?
     *
     * @param index     0-based position of this outcome in the sequence
     * @param total     the sequence length {@code n}
     * @param value     this outcome's utility
     * @param benchmark the best utility seen strictly before now ({@code −∞} if none yet)
     */
    boolean commit(int index, int total, double value, double benchmark);

    /** Short label for reports, e.g. {@code "neutral (sultan's dowry)"}. */
    default String name() { return "strategy"; }

    /** Wrap a commit rule as a named strategy. */
    static Strategy named(String name, Strategy rule) {
        return new Strategy() {
            @Override public boolean commit(int i, int n, double v, double b) { return rule.commit(i, n, v, b); }
            @Override public String name() { return name; }
        };
    }

    /**
     * A threshold strategy: observe (reject) the first {@code ratio} fraction to set a benchmark, then commit
     * to the first later outcome that beats it. The explore/commit boundary is the whole personality —
     * {@code ratio = 0} is greedy, {@code ratio = 1/e} is the dowry, {@code ratio = 1} is pure holdout.
     */
    static Strategy threshold(String name, double ratio) {
        return named(name, (i, n, v, b) -> i >= (int) Math.floor(n * ratio) && v > b);
    }

    /**
     * A strategy scaled by <b>riskiness relative to {@link #NEUTRAL}</b> (the dowry). {@code factor = 1} is
     * neutral; {@code factor > 1} leans risky and {@code factor < 1} leans safe, each doubling <b>halving the
     * remaining distance to the pole it leans toward</b>:
     *
     * <ul>
     *   <li>{@code risk(2)} — "twice as risky": splits the remaining gap from the dowry up to the {@link
     *       #RISKY} pole {@code 1.0}, landing at {@code (1 + 1/e)/2 ≈ 0.684}.</li>
     *   <li>{@code risk(0.5)} — "twice as safe": splits the gap down to the {@link #SAFE} pole {@code 0.0},
     *       landing at {@code (1/e)/2 ≈ 0.184}.</li>
     *   <li>{@code factor → ∞ ⇒ RISKY}, {@code factor → 0 ⇒ SAFE} — the poles are the limits, never crossed.</li>
     * </ul>
     */
    static Strategy risk(double factor) {
        final var ratio = factor >= 1.0
                ? 1.0 - (1.0 - SECRETARY_RATIO) / factor   // risky lean: halve the remaining distance to 1
                : SECRETARY_RATIO * factor;                // safe lean:  halve the remaining distance to 0
        return threshold(String.format("risk x%.2f (ratio=%.3f)", factor, ratio), ratio);
    }

    /** Safe pole — take the first outcome. Never waits, never ends empty, settles low (P(best) = 1/n). */
    Strategy SAFE = threshold("safe (greedy)", 0.0);

    /** The calibrated middle — the Sultan's dowry / secretary {@code 1/e} rule; maximises P(pick the best). */
    Strategy NEUTRAL = threshold("neutral (sultan's dowry)", SECRETARY_RATIO);

    /** Risky pole — observe all, hold out for the best; high variance, risks the forced last pick (P(best) = 1/n). */
    Strategy RISKY = threshold("risky (holdout)", 1.0);
}
