package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The routing contract: orbit-constant keys, a faithful context projection, and an honest reject signal. */
class RoutingTest {

    static Stream<String> datasets() { return Fixtures.ALL.stream(); }

    /**
     * The invariance contract (§3) made executable over the shipped chain. If a coordinate varied across an
     * orbit, orbit-mates would route to different contexts and the class would shatter — the one failure the
     * whole design is built to exclude.
     */
    @ParameterizedTest(name = "the chain is orbit-constant — {0}")
    @MethodSource("datasets")
    void orbitMatesRouteTogether(String stem) {
        final var d = Fixtures.load(stem);
        final var p = d.trained();
        for (final var grid : d.grids())
            for (final var t : d.group().transforms())
                assertEquals(p.descriptor(grid), p.descriptor(t.apply(grid, d.rows(), d.cols())),
                        "a " + t.label() + " image must route to the same key");
    }

    /**
     * The store keeps paths; {@code contexts()} reassembles them into keys. That projection has to be
     * faithful, or every whole-table report is reading a different world from the one {@code score} routes in.
     */
    @ParameterizedTest(name = "contexts() is a faithful projection of the store — {0}")
    @MethodSource("datasets")
    void everyTrainedGridIsFindableInTheContextView(String stem) {
        final var d = Fixtures.load(stem);
        final var p = d.trained();
        final var view = p.contexts();
        for (final var grid : d.grids()) {
            final var key = p.descriptor(grid);
            final var fromView = view.get(key);
            assertNotNull(fromView, "the key a grid routes to must appear in contexts()");
            final var direct = p.context(grid);
            assertEquals(direct.pos(), fromView.pos(), "positive counts agree");
            assertEquals(direct.neg(), fromView.neg(), "negative counts agree");
            assertEquals(direct.answer(), fromView.answer(), "answers agree");
        }
        // and nothing extra: every projected key is one some grid actually reaches
        assertEquals(d.grids().stream().map(p::descriptor).distinct().count(), view.size(),
                "the projection has exactly the trained keys, no more");
    }

    /**
     * {@code score} collapses reject onto the negative class; {@code verdict} must not. This is the defect
     * that made the Searcher count never-trained transforms among a grid's surviving symmetries.
     */
    @Test
    @DisplayName("verdict separates a judged negative from a never-seen key")
    void verdictKeepsTheRejectSignal() {
        final var d = Fixtures.load("twin-t-flip-3x3");
        final var p = d.trained();

        for (final var grid : d.grids())
            assertTrue(p.verdict(grid) != Plexus.Verdict.UNSEEN, "a trained grid is never UNSEEN");

        final var untrained = new int[9];                    // the empty grid routes nowhere in this dataset
        assertSame(Plexus.Verdict.UNSEEN, p.verdict(untrained), "an untrained key is UNSEEN, not NEGATIVE");
        assertEquals(0, p.score(untrained), "score still collapses it to 0 — that is why verdict exists");
    }

    /** Routing must depend only on injected configuration, never on what has been fitted. */
    @ParameterizedTest(name = "descriptors do not depend on fitted state — {0}")
    @MethodSource("datasets")
    void routingIsIndependentOfTraining(String stem) {
        final var d = Fixtures.load(stem);
        final var untrained = d.empty();
        final var trained = d.trained();
        for (final var grid : d.grids())
            assertEquals(untrained.descriptor(grid), trained.descriptor(grid),
                    "training must not move a grid to a different key");
    }
}
