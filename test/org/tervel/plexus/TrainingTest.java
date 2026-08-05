package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.tervel.plexus.invariants.Connectivity;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.invariants.Mass;
import org.tervel.plexus.invariants.Signature;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.symmetry.Dihedral;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Training is one pass and stays correct under it — the property the removed seal used to prevent. */
class TrainingTest {

    static Stream<String> datasets() { return Fixtures.ALL.stream(); }

    /**
     * The no-seal property, stated as strongly as it can be: insert one example at a time and after
     * <em>every</em> insert, every grid seen so far must still resolve to its own label. A frozen exception
     * list cannot do this — a majority that flips mid-stream leaves the sealed list wrong — which is exactly
     * why {@code fit} used to need a second pass.
     */
    @ParameterizedTest(name = "online inserts stay exact at every step — {0}")
    @MethodSource("datasets")
    void everyPrefixOfTheStreamResolvesExactly(String stem) {
        final var d = Fixtures.load(stem);
        final var p = d.empty();
        for (var i = 0; i < d.grids().size(); i++) {
            p.accept(d.grids().get(i), d.labels().get(i));
            for (var j = 0; j <= i; j++)
                assertEquals(d.labels().get(j), p.score(d.grids().get(j)),
                        "grid " + j + " went stale after insert " + i);
        }
    }

    /** Batch and streamed training must agree — one pass is the only pass. */
    @ParameterizedTest(name = "fit == a stream of accepts — {0}")
    @MethodSource("datasets")
    void batchAgreesWithStreaming(String stem) {
        final var d = Fixtures.load(stem);
        final var batch = d.trained();
        final var streamed = d.empty();
        for (var i = 0; i < d.grids().size(); i++) streamed.accept(d.grids().get(i), d.labels().get(i));
        for (final var grid : d.grids())
            assertEquals(batch.score(grid), streamed.score(grid), "batch and streamed answers must agree");
        assertEquals(batch.contexts().size(), streamed.contexts().size(), "and produce the same table");
    }

    /**
     * Regression. A grid seen under <b>both</b> labels used to be sealed as an exception once per
     * contradicting example, which made {@code isException} true and flipped it away from the label it
     * mostly carries — a grid 3:2 in favour of 1 answered 0. Deriving exceptions by comparing the member's
     * own majority to its leaf's answers it 1.
     */
    @Test
    @DisplayName("a grid seen under both labels answers with its own majority, not against it")
    void mixedLabelGridIsNotFlippedAgainstItself() {
        final var grid = new int[]{1, 1, 0, 0, 0, 0, 0, 0, 0};
        final List<Invariant> chain = List.of(new Signature(new Mass(), new Connectivity(), 3));
        final var p = new Plexus(Dihedral.d4(), chain, new ExactExceptions(), 3, 3);

        final var grids = new ArrayList<int[]>();
        final var labels = new ArrayList<Integer>();
        for (var i = 0; i < 3; i++) { grids.add(grid); labels.add(1); }
        for (var i = 0; i < 2; i++) { grids.add(grid); labels.add(0); }
        p.fit(grids, labels);

        final var c = p.context(grid);
        assertEquals(3, c.pos());
        assertEquals(2, c.neg());
        assertEquals(1, c.answer(), "the context's majority is 1");
        assertEquals(0, c.exceptionCount(), "the grid does not contradict its own majority");
        assertEquals(1, p.score(grid), "so it must answer 1, not be flipped to 0");
    }

    /** An exception is a member that genuinely dissents — and it is answered exactly, against the majority. */
    @Test
    @DisplayName("a dissenting member is answered against its context's majority")
    void genuineDissentIsResolvedExactly() {
        final var d = Fixtures.load("twin-t-flip-3x3");
        final var p = d.trained();
        var dissenters = 0;
        for (var i = 0; i < d.grids().size(); i++) {
            final var c = p.context(d.grids().get(i));
            if (d.labels().get(i) != c.answer()) {
                dissenters++;
                assertTrue(c.isException(d.grids().get(i)), "a contradicting grid must be an exception");
                assertEquals(d.labels().get(i), p.score(d.grids().get(i)), "and resolve to its own label");
            }
        }
        assertEquals(1, dissenters, "this dataset plants exactly one contradiction");
    }
}
