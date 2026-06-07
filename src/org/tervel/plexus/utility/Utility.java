package org.tervel.plexus.utility;

import java.util.function.ToDoubleFunction;

/**
 * A <b>utility function</b>: the real-valued worth of an outcome. It is the decision-theoretic peer of the
 * router's other concepts, and it follows the same idiom — each concept <em>is</em> a JDK functional
 * interface ({@code Invariant extends ToIntFunction}, {@code Transform extends UnaryOperator}, {@code
 * Operation extends Function}) — so a {@code Utility} <em>is</em> a {@link ToDoubleFunction}{@code <T>} and
 * {@link #applyAsDouble} is the payoff.
 *
 * <p>Where an {@link org.tervel.plexus.invariants.Invariant} answers "what coordinate is this?", a utility
 * answers "how good is this?". The {@link org.tervel.plexus.residual.ResidualResolver} already makes an
 * implicit choice — it picks the label that minimises error, the argmax under a hidden 0/1 utility — so this
 * interface only makes that valuation explicit and reusable by any selection policy (e.g. {@link OptimalStop}).
 *
 * @param <T> the kind of outcome being valued (a grid, a context key, a transform, a dowry…)
 */
@FunctionalInterface
public interface Utility<T> extends ToDoubleFunction<T> {

    /** The payoff of an outcome (the {@link ToDoubleFunction} of the utility). */
    @Override
    double applyAsDouble(T outcome);

    /** Short label for reports, e.g. {@code "fiber-volume"}. */
    default String name() { return "utility"; }

    /** Wrap a scoring function as a named {@code Utility}. */
    static <T> Utility<T> of(String name, ToDoubleFunction<T> score) {
        return new Utility<>() {
            @Override public double applyAsDouble(T outcome) { return score.applyAsDouble(outcome); }
            @Override public String name() { return name; }
        };
    }
}
