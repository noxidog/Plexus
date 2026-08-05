package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tervel.plexus.diagnostics.ChainCritic;
import org.tervel.plexus.invariants.Connectivity;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.invariants.Mass;
import org.tervel.plexus.invariants.MaxDegree;
import org.tervel.plexus.invariants.Position;
import org.tervel.plexus.invariants.Signature;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.symmetry.Dihedral;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The critic's grades, pinned on datasets whose answer is known by construction. */
class ChainCriticTest {

    private static ChainCritic.Critique grade(String stem, List<Invariant> chain, String name) {
        final var d = Fixtures.load(stem);
        final var p = new Plexus(d.group(), chain, new ExactExceptions(), d.rows(), d.cols());
        p.fit(d.grids(), d.labels());
        return new ChainCritic(d.grids(), d.labels()).critique(p).stream()
                .filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    /**
     * {@code Position} is deliberately not orbit-constant — it is the documented radial-only exception, the
     * axis that dissolves dot-position contradictions. The critic must recognise that rather than condemn it,
     * and must name the witness.
     */
    @Test
    @DisplayName("Position grades ORIENTING, with a concrete witness")
    void radialScopedBreakIsNotCalledIllegal() {
        final var chain = List.<Invariant>of(new Signature(new Mass(), new Connectivity(), 3), new Position());
        final var c = grade("position-corner-dot-3x3", chain, "quadrant");
        assertEquals(ChainCritic.Grade.ORIENTING, c.grade());
        assertTrue(c.detail().contains("varies over an orbit"), "the break is reported");
        assertTrue(c.detail().contains("under "), "with the transform that witnesses it: " + c.detail());
    }

    /** MaxDegree is exactly what the path-vs-fork collision earns, so it must grade as carrying its weight. */
    @Test
    @DisplayName("MaxDegree grades OK where a collision earns it")
    void anEarnedCoordinateGradesOk() {
        final var chain = List.<Invariant>of(new Signature(new Mass(), new Connectivity(), 9), new MaxDegree());
        final var c = grade("renormalization-path-fork-9x9", chain, "deg");
        assertEquals(ChainCritic.Grade.OK, c.grade());
        assertTrue(c.collisionsAdded() > 0, "dropping it must cost at least one collision");
    }

    /**
     * Two copies of a coordinate are individually droppable, so both grade REDUNDANT. This also pins the fix
     * for an ablation that omitted by <em>name</em> and so removed both copies at once, hiding the redundancy.
     */
    @Test
    @DisplayName("a duplicated coordinate grades REDUNDANT — ablation is positional, not by name")
    void duplicateCoordinateIsDetected() {
        final var d = Fixtures.load("renormalization-path-fork-9x9");
        final List<Invariant> chain = List.of(
                new Signature(new Mass(), new Connectivity(), 9), new MaxDegree(), new MaxDegree());
        final var p = new Plexus(d.group(), chain, new ExactExceptions(), d.rows(), d.cols());
        p.fit(d.grids(), d.labels());
        final var grades = new ChainCritic(d.grids(), d.labels()).critique(p);
        assertEquals(3, grades.size(), "one report card per declared coordinate");
        assertEquals(ChainCritic.Grade.REDUNDANT, grades.get(1).grade());
        assertEquals(ChainCritic.Grade.REDUNDANT, grades.get(2).grade());
    }

    /** On a single-orbit dataset the signature does no work — true of the data, not a fault in the axis. */
    @Test
    @DisplayName("a coordinate with one value over the data grades INERT")
    void narrowDataMakesACoordinateInert() {
        final var chain = List.<Invariant>of(new Signature(new Mass(), new Connectivity(), 3));
        final var c = grade("conservation-t-orbit-3x3", chain, "sig");
        assertEquals(ChainCritic.Grade.INERT, c.grade());
        assertEquals(1, c.distinct(), "one value across the whole dataset");
    }

    /** The verdict that matters most is the one that says stop: an orbit-mate collision has no repair. */
    @Test
    @DisplayName("a provable twin is reported as irreducible, and a radial one names its escape")
    void twinVerdictsAreReported() {
        final var d = Fixtures.load("twin-t-flip-3x3");
        final var oriented = new ChainCritic(d.grids(), d.labels()).apply(d.trained());
        assertTrue(oriented.contains("IRREDUCIBLE"), "an orbit-mate collision cannot be split");

        final var r = Fixtures.load("position-corner-dot-3x3");
        final var radial = new ChainCritic(r.grids(), r.labels()).apply(r.trained());
        assertTrue(radial.contains("IRREDUCIBLE"), "the radial collision is also a twin");
        assertTrue(radial.contains("RADIAL"), "but radial is the one twin verdict with a way out");
    }

    /** Sector chains: a withheld coordinate must route in the rich sector and nowhere else. */
    @Test
    @DisplayName("byOrbit withholds a coordinate from low-symmetry sectors only")
    void perSectorChainsSelectByOrbit() {
        final var d = Fixtures.load("renormalization-path-fork-9x9");
        final var deg = new MaxDegree();
        final List<Invariant> declared = List.of(new Signature(new Mass(), new Connectivity(), 9), deg);
        final var policy = Plexus.ChainPolicy.byOrbit(Dihedral.d4(), 4, List.of(deg));
        final var p = new Plexus(d.group(), declared, new ExactExceptions(), d.rows(), d.cols(), policy);
        p.fit(d.grids(), d.labels());

        final var order = d.group().transforms().size();
        for (final var grid : d.grids()) {
            final var sector = p.sector(grid);
            final var orbit = order / Integer.bitCount(sector.stabilizer());
            assertEquals(orbit <= 4, p.chainFor(sector).contains(deg),
                    "deg routes exactly in sectors whose orbit is at most 4");
            // and selecting by sector can never split an orbit
            for (final var t : d.group().transforms())
                assertEquals(p.descriptor(grid), p.descriptor(t.apply(grid)), "orbit-mates stay together");
        }
    }
}
