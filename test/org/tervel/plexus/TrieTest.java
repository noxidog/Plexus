package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.tervel.plexus.trie.Trie;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two trie configurations. The fixed-chain one backs the router, so its answers must match the router's;
 * the self-extending one is judged on whether memory really shrinks to the twin floor.
 */
class TrieTest {

    static Stream<String> datasets() { return Fixtures.ALL.stream(); }
    static Stream<String> squareDatasets() { return Fixtures.SQUARE.stream(); }

    /** Laziness is an optimisation, so it must never change an answer on a trained key. */
    @ParameterizedTest(name = "the lazy walk agrees with the router — {0}")
    @MethodSource("datasets")
    void pureStopsDoNotChangeAnswers(String stem) {
        final var d = Fixtures.load(stem);
        final var p = d.trained();
        final var trie = new Trie(p);
        trie.fit(d.grids(), d.labels());
        for (final var grid : d.grids()) {
            final var r = trie.resolve(grid);
            assertEquals(p.score(grid), r.label(), "a pure-stop must not change the label");
            assertTrue(r.exact(), "a trained grid resolves exactly, never by backoff");
            assertTrue(r.evals() <= r.fullDepth(), "laziness can only skip work, never add it");
        }
    }

    /** Leaf enumeration is what the router's context projection is built on, so the counts must line up. */
    @ParameterizedTest(name = "leaves correspond to contexts — {0}")
    @MethodSource("datasets")
    void leafCountMatchesContextCount(String stem) {
        final var d = Fixtures.load(stem);
        final var p = d.trained();
        final var leaves = new int[1];
        final var trie = new Trie(p);
        trie.fit(d.grids(), d.labels());
        trie.forEachLeaf((radial, stab, coords, counts, dissenters) -> leaves[0]++);
        assertEquals(p.contexts().size(), leaves[0], "one leaf per context");
    }

    /**
     * The design claim, measured rather than asserted: where a collision is separable the trie grows an edge
     * and memorises nothing; where it is a provable twin exactly one row survives. Memory shrinks to the twin
     * floor and no further.
     */
    @ParameterizedTest(name = "extension pushes memory down to the twin floor — {0}")
    @MethodSource("squareDatasets")
    void memoryShrinksToTheTwinFloor(String stem) {
        final var d = Fixtures.load(stem);
        final var p = d.trained();
        final var trie = Trie.growing(p);
        trie.fit(d.grids(), d.labels());

        for (var i = 0; i < d.grids().size(); i++) {
            final var r = trie.resolve(d.grids().get(i));
            if (r.label() == d.labels().get(i)) continue;
            assertTrue(r.irreducible(),
                    "a wrong answer is only allowed at a provable-twin leaf, never elsewhere");
        }
        assertTrue(trie.irreducibleLeaves() <= trie.storedRows(), "irreducible leaves are a subset of rows");
    }

    /** The documented numbers (§5). If these drift, the design doc is lying and this test says so. */
    @Test
    @DisplayName("the recorded extension results still hold")
    void documentedExtensionResults() {
        //             dataset                          sealed  rows  splits  irreducible
        assertExtension("renormalization-path-fork-9x9",      5,    3,      1,           0);
        assertExtension("separable-path-fork-5x5",            1,    2,      1,           0);
        assertExtension("twin-t-flip-3x3",                    1,    1,      0,           1);
        assertExtension("position-corner-dot-3x3",            1,    1,      0,           1);
        assertExtension("conservation-t-orbit-3x3",           0,    1,      0,           0);
        assertExtension("entropy-fiber-gap-5x5",              0,    1,      0,           0);
    }

    /** One row of the §5 table, every column of it, so the published figures cannot drift unnoticed. */
    private static void assertExtension(String stem, int sealed, int rows, int splits, int irreducible) {
        final var d = Fixtures.load(stem);
        final var p = d.trained();
        final var trie = Trie.growing(p);
        trie.fit(d.grids(), d.labels());
        assertEquals(sealed, p.contexts().values().stream().mapToInt(Plexus.Context::exceptionCount).sum(),
                stem + ": exceptions the router had to memorise");
        assertEquals(rows, trie.storedRows(), stem + ": rows the extending store kept");
        assertEquals(splits, trie.splits(), stem + ": local extensions the data forced");
        assertEquals(irreducible, trie.irreducibleLeaves(), stem + ": leaves nothing could split");
        if (splits > 0)
            assertEquals(0, trie.irreducibleLeaves(),
                    stem + ": a separable collision must leave no memory behind");
    }

    /**
     * The measured negative result of §5, pinned so it cannot quietly become a positive one. Hold out every
     * example of a key, refit, and ask the trie: it walks as deep as the remaining data reaches and answers
     * from the deepest seen ancestor. On a <b>minimal</b> chain that is systematically wrong, because an
     * invariant only enters the chain when it flips labels inside a prefix bucket — so the deepest rung is
     * always load-bearing and the prefix majority is anti-correlated exactly where the full key matters.
     *
     * <p>If someone "improves" backoff into something that works here, this test fails and the design doc's
     * claim has to be rewritten rather than silently falsified.
     */
    @Test
    @DisplayName("backoff on a held-out key is unreliable — a minimal chain has treacherous prefixes")
    void backoffIsWrongWhereEveryCoordinateWasEarned() {
        final var d = Fixtures.load("renormalization-path-fork-9x9");
        // The documented protocol runs on a chain that has EARNED its last coordinate: max-degree is what the
        // collision diagnostic emits for this dataset. That is the whole point — the claim is about a minimal
        // chain, so a chain missing the earned axis would not test it.
        final java.util.function.Supplier<Plexus> build = () -> new Plexus(d.group(),
                List.of(d.chain().get(0), new org.tervel.plexus.invariants.MaxDegree()),
                new org.tervel.plexus.residual.ExactExceptions(), d.rows(), d.cols());

        final var full = build.get();
        full.fit(d.grids(), d.labels());
        var held = 0;
        var correct = 0;
        for (final var key : d.grids().stream().map(full::descriptor).distinct().toList()) {
            final var restG = new java.util.ArrayList<int[]>();
            final var restL = new java.util.ArrayList<Integer>();
            final var outIdx = new java.util.ArrayList<Integer>();
            for (var i = 0; i < d.grids().size(); i++) {
                if (full.descriptor(d.grids().get(i)).equals(key)) outIdx.add(i);
                else { restG.add(d.grids().get(i)); restL.add(d.labels().get(i)); }
            }
            if (restG.isEmpty()) continue;
            final var reduced = build.get();
            reduced.fit(restG, restL);
            final var trie = new Trie(reduced);
            trie.fit(restG, restL);
            for (final var i : outIdx) {
                held++;
                final var r = trie.resolve(d.grids().get(i));
                if (!r.rejected() && r.label() == d.labels().get(i)) correct++;
            }
        }
        assertEquals(12, held, "every example is held out across the three keys");
        assertEquals(0, correct, "backoff gets every held-out grid wrong on this dataset (§5)");
    }

    /**
     * The census reads the store's <b>prefixes</b>, which is the one thing the context view cannot show:
     * contexts are leaves, so a coordinate that decided nothing is invisible there and shows up here as a
     * <b>redundant</b> node. Pins the §5 figures for the rectangular set, where two of three stabilizer
     * nodes are already pure and the signature rung is therefore redundant for both.
     */
    @Test
    @DisplayName("the census reports the documented prefix structure")
    void censusExposesRedundantRungs() {
        final var report = org.tervel.plexus.reports.TrieReport.census(
                Fixtures.load("rect-bar-2x4").trained().store());
        assertTrue(report.contains("stab                   3       2          0"),
                "3 stabilizer nodes, 2 already pure, none redundant:\n" + report);
        assertTrue(report.contains("sig                    5       5          2"),
                "5 signature nodes, all pure, 2 of them redundant:\n" + report);
    }

    /** Leaf order must not depend on hash iteration, or every report becomes irreproducible. */
    @ParameterizedTest(name = "leaf order is deterministic — {0}")
    @MethodSource("datasets")
    void enumerationOrderIsStable(String stem) {
        final var d = Fixtures.load(stem);
        final var first = String.join("|", d.trained().contexts().keySet().stream().map(Object::toString).toList());
        for (var run = 0; run < 3; run++) {
            final var again = String.join("|", d.trained().contexts().keySet().stream().map(Object::toString).toList());
            assertEquals(first, again, "context enumeration must be identical run to run");
        }
        assertFalse(first.isEmpty(), "and non-empty");
    }
}
