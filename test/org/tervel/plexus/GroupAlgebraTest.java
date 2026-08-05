package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.tervel.plexus.symmetry.Dihedral;
import org.tervel.plexus.symmetry.SymmetryGroup;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The multiplication table has to actually be a group. Everything derived from it — closure, canonical
 * spellings, the impossible/dead/unpopulated split — is only as sound as these four axioms.
 */
class GroupAlgebraTest {

    static Stream<SymmetryGroup> groups() { return Stream.of(Dihedral.d4(), Dihedral.d2()); }

    @ParameterizedTest(name = "closure, identity, inverses, associativity — {0}")
    @MethodSource("groups")
    void satisfiesTheGroupAxioms(SymmetryGroup g) {
        final var n = g.transforms().size();
        final var table = g.multiplicationTable();
        final var e = g.identityIndex();

        for (var a = 0; a < n; a++) {
            assertTrue(table[a * n + e] == a && table[e * n + a] == a, "identity is a unit for " + a);
            assertEquals(e, g.compose(a, g.inverse(a)), "a∘a⁻¹ = e");
            for (var b = 0; b < n; b++) {
                final var ab = table[a * n + b];
                assertTrue(ab >= 0 && ab < n, "composition stays inside the set (closure)");
                for (var c = 0; c < n; c++)                       // (a∘b)∘c == a∘(b∘c)
                    assertEquals(g.compose(ab, c), g.compose(a, g.compose(b, c)), "associativity");
            }
        }
    }

    @Test
    @DisplayName("a non-closed mask is impossible, and closure names exactly what it forces")
    void closureRepairsANonSubgroup() {
        final var g = Dihedral.d4();
        final var ts = g.transforms();
        final var mirrorH = 1 << indexOf(g, "mirrorH");
        final var mirrorV = 1 << indexOf(g, "mirrorV");
        final var rot180 = 1 << indexOf(g, "rot180");
        final var identity = 1 << g.identityIndex();

        final var requested = identity | mirrorH | mirrorV;
        assertFalse(g.isSubgroup(requested), "mirrorH ∘ mirrorV = rot180, so this is not closed");
        assertEquals(requested | rot180, g.closure(requested), "closure adds exactly rot180");
        assertTrue(g.isSubgroup(g.closure(requested)), "the closure is a subgroup");
        assertEquals(ts.size(), Integer.bitCount(g.closure(identity | mirrorH | (1 << indexOf(g, "diagMain")))),
                "a mirror and a diagonal generate all of D4");
    }

    @Test
    @DisplayName("under D4 mirrorV is a dead spelling; under D2 it is live")
    void canonicalSpellingFoldsOnlyWhatConjugationMoves() {
        final var d4 = Dihedral.d4();
        final var d2 = Dihedral.d2();
        final var h4 = (1 << d4.identityIndex()) | (1 << indexOf(d4, "mirrorH"));
        final var v4 = (1 << d4.identityIndex()) | (1 << indexOf(d4, "mirrorV"));
        // rot90 conjugation swaps the axis mirrors, so they are one class and the minimum picks mirrorH.
        assertEquals(h4, d4.canonicalMask(v4), "mirrorV canonicalises to mirrorH under D4");
        assertEquals(h4, d4.canonicalMask(h4), "canonicalMask is idempotent");

        // D2's only rotation is rot180, which commutes with both mirrors, so nothing folds.
        final var h2 = (1 << d2.identityIndex()) | (1 << indexOf(d2, "mirrorH"));
        final var v2 = (1 << d2.identityIndex()) | (1 << indexOf(d2, "mirrorV"));
        assertEquals(v2, d2.canonicalMask(v2), "mirrorV stays live under D2 — the transpose break buys vocabulary");
        assertFalse(h2 == v2, "and the two spellings remain distinct");
    }

    @ParameterizedTest(name = "canonicalMask agrees with canonicalStabilizer — {0}")
    @MethodSource("org.tervel.plexus.GroupAlgebraTest#squareDatasets")
    void maskLevelFoldMatchesTheGridLevelFold(String stem) {
        final var d = Fixtures.load(stem);
        for (final var grid : d.grids())
            assertEquals(d.group().canonicalStabilizer(grid, d.rows(), d.cols()),
                    d.group().canonicalMask(d.group().stabilizer(grid, d.rows(), d.cols())),
                    "folding the mask by conjugacy = folding the grid by rotation");
    }

    static Stream<String> squareDatasets() { return Fixtures.ALL.stream(); }

    private static int indexOf(SymmetryGroup g, String label) {
        final List<?> ts = g.transforms();
        for (var i = 0; i < ts.size(); i++)
            if (g.transforms().get(i).label().equals(label)) return i;
        assertNotNull(null, "no such transform: " + label);
        return -1;
    }
}
