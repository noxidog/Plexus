package org.tervel.plexus;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * One step of a {@link Pipeline}: a context and a grid in, a grid out. The output may be the input unchanged;
 * a caller assumes nothing either way and hands it to the next stage.
 *
 * <p>The context is whatever the caller resolves its stages against: what was bound before this grid
 * existed and outlives it. Plexus never looks inside it; it hands the same context to every stage, test and
 * effect of one application, so each callback sees exactly what the caller resolved above it.
 *
 * @param <C> the caller's context.
 */
@FunctionalInterface
public interface Stage<C> {

    /** Apply this stage to {@code grid} in {@code context}. */
    Grid apply(C context, Grid grid);

    /** A condition on one cell of a grid. */
    @FunctionalInterface
    interface Test<C> { boolean holds(C context, Grid grid, int row, int col); }

    /** A change at one cell, returning the changed grid. */
    @FunctionalInterface
    interface Effect<C> { Grid apply(C context, Grid grid, int row, int col); }

    /**
     * At every cell of {@code anchor}, in reading order: where {@code test} holds <b>on the input</b>,
     * apply {@code effect} to the output. Every test reads the grid the stage was given, so the order the
     * cells are visited in cannot change the result.
     */
    static <C> Stage<C> atEach(String anchor, Test<C> test, Effect<C> effect) {
        return (ctx, in) -> {
            var out = in;
            for (final var rc : in.cells(anchor))
                if (test.holds(ctx, in, rc[0], rc[1])) out = effect.apply(ctx, out, rc[0], rc[1]);
            return out;
        };
    }

    /**
     * At every cell of {@code anchor}, in reading order, <b>writing through</b>: each test reads the grid as
     * the stage has written it so far, so a change at one cell is seen by every cell visited after it. A
     * cell that an earlier change took out of {@code anchor} is skipped.
     */
    static <C> Stage<C> sweep(String anchor, Test<C> test, Effect<C> effect) {
        return (ctx, in) -> {
            var out = in;
            for (final var rc : in.cells(anchor)) {
                if (!out.in(anchor, rc[0], rc[1])) continue;
                if (test.holds(ctx, out, rc[0], rc[1])) out = effect.apply(ctx, out, rc[0], rc[1]);
            }
            return out;
        };
    }

    /** This stage where {@code when} holds for the context and input, the input unchanged otherwise. */
    default Stage<C> onlyIf(BiPredicate<C, Grid> when) {
        return (ctx, g) -> when.test(ctx, g) ? apply(ctx, g) : g;
    }

    /** These stages in order, each one's output the next one's input, all in one context. */
    static <C> Pipeline<C> of(List<Stage<C>> stages) { return new Pipeline<>(stages); }
}
