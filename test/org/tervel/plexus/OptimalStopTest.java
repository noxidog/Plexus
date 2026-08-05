package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tervel.plexus.utility.OptimalStop;
import org.tervel.plexus.utility.Strategy;
import org.tervel.plexus.utility.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deciding without the volume (§11): the explore-vs-commit dial, and why its neutral point is not a
 * compromise but the <b>fixed point of two regrets</b>.
 *
 * <p>The claim was previously produced by a runner that printed a table nobody checked. It is deterministic —
 * the Monte Carlo is seeded — so it can simply be asserted: the dowry ratio must beat both poles, and the
 * poles must collapse to roughly {@code 1/n}. A change that "improved" a pole past neutral would mean the
 * §11 reading is wrong, and this says so.
 */
class OptimalStopTest {

    private static final int N = 20;
    private static final int TRIALS = 20_000;
    private static final Utility<Double> WORTH = Utility.of("dowry", Double::doubleValue);

    /** Marry-the-best rate over identical seeded arrival orders — the same race the runner used to print. */
    private static double rate(Strategy strategy) {
        final var rng = new Random(42);                        // identical sequences per strategy: a fair race
        var wins = 0;
        for (var t = 0; t < TRIALS; t++) {
            final var sequence = new ArrayList<Double>(N);
            for (var i = 0; i < N; i++) sequence.add(rng.nextDouble());
            final var choice = new OptimalStop<>(WORTH, strategy).apply(sequence);
            if (choice != null && choice.optimal()) wins++;
        }
        return (double) wins / TRIALS;
    }

    @Test
    @DisplayName("the dowry ratio lands near 1/e and strictly beats both poles")
    void neutralIsTheOptimum() {
        final var safe = rate(Strategy.SAFE);
        final var neutral = rate(Strategy.NEUTRAL);
        final var risky = rate(Strategy.RISKY);

        assertEquals(1.0 / Math.E, neutral, 0.02, "NEUTRAL is the 1/e rule, so it lands near 0.368");
        assertTrue(neutral > safe * 3, "and dominates the risk-averse pole, not merely edges it");
        assertTrue(neutral > risky * 3, "and the risk-seeking pole likewise");
        assertEquals(1.0 / N, safe, 0.02, "SAFE takes the first arrival: 1/n");
        assertEquals(1.0 / N, risky, 0.02, "RISKY waits for a best that usually never comes: 1/n");
    }

    /**
     * The dial is not a compromise: it is a maximum, so moving <em>either</em> way from neutral costs. That
     * is the §11 point — the poles fall away from a fixed point rather than bracketing a midpoint.
     */
    @Test
    @DisplayName("moving off neutral in either direction loses")
    void theDialFallsAwayFromItsFixedPoint() {
        final var neutral = rate(Strategy.NEUTRAL);
        assertTrue(rate(Strategy.risk(0.5)) < neutral, "leaning safe costs");
        assertTrue(rate(Strategy.risk(2)) < neutral, "leaning risky costs");
    }

    /**
     * {@code risk(factor)} halves the remaining distance to the pole it leans toward (§11). The ratio is not
     * exposed as an accessor, so it is recovered behaviourally: sweep the arrival index with a value that
     * beats any benchmark and find where the strategy first commits — that boundary is the observation
     * window, i.e. the ratio.
     */
    private static double observedRatio(Strategy s) {
        final var n = 100_000;
        for (var i = 0; i < n; i++) if (s.commit(i, n, 1.0, 0.0)) return (double) i / n;
        return 1.0;                                            // never commits: the holdout pole
    }

    @Test
    @DisplayName("risk(2) sits halfway from the dowry ratio to pure holdout")
    void riskScalesRelativeToNeutral() {
        assertEquals(Strategy.SECRETARY_RATIO, observedRatio(Strategy.NEUTRAL), 1e-4);
        assertEquals((1 + Strategy.SECRETARY_RATIO) / 2, observedRatio(Strategy.risk(2)), 1e-4,
                "twice as risky splits the gap up to the holdout pole");
        assertEquals(Strategy.SECRETARY_RATIO / 2, observedRatio(Strategy.risk(0.5)), 1e-4,
                "twice as safe splits the gap down to the greedy pole");
        assertEquals(0.0, observedRatio(Strategy.SAFE), 1e-4, "the poles are the limits");
        assertTrue(observedRatio(Strategy.risk(1000)) < 1.0, "and are approached, never crossed");
    }
}
