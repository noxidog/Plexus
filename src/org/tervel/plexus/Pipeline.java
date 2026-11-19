package org.tervel.plexus;

import java.util.List;

/**
 * Stages run in order, each one handed the grid the previous one returned and the one context they all
 * share: a fold of the grid through the stages, applied once. A pipeline is itself a stage, so pipelines
 * nest.
 *
 * @param <C> the caller's context.
 */
public record Pipeline<C>(List<Stage<C>> stages) implements Stage<C> {

    public Pipeline { stages = List.copyOf(stages); }

    @Override public Grid apply(C context, Grid grid) {
        var g = grid;
        for (final var s : stages) g = s.apply(context, g);
        return g;
    }
}
