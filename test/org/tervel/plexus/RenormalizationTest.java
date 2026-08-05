package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tervel.plexus.diagnostics.CollisionDiagnostic;
import org.tervel.plexus.invariants.Connectivity;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.invariants.Mass;
import org.tervel.plexus.invariants.Signature;
import org.tervel.plexus.ops.Scale;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.symmetry.Dihedral;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two-scale closure loop (§11, <i>change of scale</i>): is a structural law the same at every scale, or
 * does it shatter under coarse-graining?
 *
 * <p>The conclusion was previously computed and <b>printed</b> by a runner, which meant a change of verdict
 * would have gone unnoticed — the same defect the trie runners had. Here it is asserted: the fine scale must
 * stay reducible (a separable collision, {@code max-degree} blueprint, floor 0) and the coarse scale must
 * shatter into an irreducible floor. If coarse-graining ever stops destroying the distinction, this fails and
 * §11's claim needs rewriting rather than quietly becoming false.
 */
class RenormalizationTest {

    private static final int FINE = 9;
    private static final int K = 3;

    private static List<Invariant> chain(int side) {
        return List.of(new Signature(new Mass(), new Connectivity(), side));
    }

    private static Plexus fit(int side, List<int[]> grids, List<Integer> labels) {
        final var p = new Plexus(Dihedral.d4(), chain(side), new ExactExceptions(), side, side);
        p.fit(grids, labels);
        return p;
    }

    @Test
    @DisplayName("the fine scale is reducible; the coarse scale shatters into an irreducible floor")
    void coarseGrainingManufacturesTwins() {
        final var d = Fixtures.load("renormalization-path-fork-9x9");

        final var fineP = fit(FINE, d.grids(), d.labels());
        final var fine = new CollisionDiagnostic(d.grids(), d.labels()).analyze(fineP);
        assertTrue(fine.collisions() > 0, "the fine scale poses a collision at all");
        assertEquals(0, fine.twinContexts(), "and it is NOT irreducible at this scale");
        assertEquals(fine.collisions(), fine.separableContexts(), "every fine collision is separable");
        assertTrue(fine.blueprint().contains("max-degree"),
                "the fine collision is a path-vs-fork, so the battery names max-degree: " + fine.blueprint());

        // Fold each grid by k×k block decimation, then re-ask the same question of the coarser world.
        final var coarseGrids = new ArrayList<int[]>();
        for (final var g : d.grids())
            coarseGrids.add(new Scale(g, K, Scale.Rule.CANONICAL).apply(fineP).coarse());
        final var coarseP = fit(FINE / K, coarseGrids, d.labels());
        final var coarse = new CollisionDiagnostic(coarseGrids, d.labels()).analyze(coarseP);

        assertTrue(coarse.twinContexts() > fine.twinContexts(),
                "the fold must RAISE the irreducible floor — that is what shattering means");
        assertEquals(0, coarse.separableContexts(), "and leave nothing the battery can still separate");
        assertTrue(coarse.blueprint().isEmpty(),
                "so no counterterm is available at the coarse scale: " + coarse.blueprint());
    }

    /** The fold has to actually discard degrees of freedom, or the experiment is vacuous. */
    @Test
    @DisplayName("block decimation integrates out microstates")
    void theFoldDiscardsDegreesOfFreedom() {
        final var d = Fixtures.load("renormalization-path-fork-9x9");
        final var p = fit(FINE, d.grids(), d.labels());
        var integrated = 0.0;
        for (final var g : d.grids()) integrated += new Scale(g, K, Scale.Rule.CANONICAL).apply(p).integratedEntropy();
        assertTrue(integrated > 0, "coarse-graining must integrate out a positive number of bits");
    }
}
