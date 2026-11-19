package org.tervel.plexus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plexus: lattice pick, pose-free keys, the four-state read and how far it extrapolates, extension, and slices. */
class PlexusTest {

    static Stream<String> datasets() { return Fixtures.ALL.stream(); }

    // ---- the descriptor ------------------------------------------------------------------------------

    @Test
    @DisplayName("lattice pick: rectangles and the plus stay square, the hex disk goes hex")
    void latticePick() {
        assertEquals(Kind.RECT, describe("xxx/.x./...", 3).kind());
        assertEquals(Kind.SQUARE, describe(".x./xxx/.x.", 3).kind(), "plus: 8 square symmetries beat 4 hex");
        assertEquals(Kind.SQUARE, describe("xxx/xxx/xxx", 3).kind(), "solid block: anti-diagonal width 5, not 3");
        final var a = describe(".xx/xxx/xx.", 3);
        final var b = describe("xx./xxx/.xx", 3);
        assertEquals(Kind.HEX, a.kind());
        assertEquals(Root.D6, a.root());
        assertEquals(Kind.HEX, b.kind(), "the disk turned 90° shears the other way and is the same hexagon");
        assertEquals(a.shapeKey(), b.shapeKey());
        assertNotEquals(a.orientation(), b.orientation(), "the shift direction tells the two poses apart");
        assertEquals(9, a.symmetries().size(), "every D6 question answers yes for the disk");
        assertEquals(Kind.EMPTY, describe(".../.../...", 3).kind());
    }

    @Test
    @DisplayName("SQUARE_4 alone keeps every shape on the square lattice and the D6 root empty")
    void squareLatticeOnly() {
        final var square4 = Set.of(Lattice.SQUARE_4);
        final var disk = grid(".xx/xxx/xx.", 3);
        final var dot = grid("x../.../...", 3);
        assertEquals(Kind.HEX, Descriptor.of(disk, 3, 3).kind());
        final var d = Descriptor.of(disk, 3, 3, square4);
        assertEquals(Kind.SQUARE, d.kind());
        assertEquals(Root.D4, d.root());
        assertEquals(Lattice.SQUARE_4, d.lattice());
        assertEquals(List.of("rot180", "diagMain", "diagAnti"), d.symmetries(), "its square symmetries, not hex ones");
        assertEquals(Kind.SQUARE, Descriptor.of(dot, 3, 3, square4).kind());

        // every 3x3 grid stays on D4, and the rest of the key is what the default gives a square-filed shape
        for (var mask = 1; mask < 512; mask++) {
            final var g = new int[9];
            for (var i = 0; i < 9; i++) g[i] = (mask >> i) & 1;
            final var sq = Descriptor.of(g, 3, 3, square4);
            assertEquals(Root.D4, sq.root());
            final var dflt = Descriptor.of(g, 3, 3);
            if (dflt.root() == Root.D4) assertEquals(dflt.shapeKey(), sq.shapeKey(), "unchanged where hex was not chosen");
        }

        final var router = new Plexus(3, 3, Lattice.SQUARE_4);
        assertEquals(square4, router.lattices());
        router.accept(disk, 1);
        router.accept(dot, 0);
        assertEquals(Verdict.POSITIVE, router.read(disk).verdict());
        assertEquals(Verdict.NEGATIVE, router.read(dot).verdict());
        assertEquals(0, router.slice(Slice.on(Root.D6), (g, c, e) -> { }).confirmed(), "nothing under D6");
        assertEquals(2, router.slice(Slice.on(Root.D4).extrapolate(Extrapolate.NONE), (g, c, e) -> { }).confirmed());
    }

    @Test
    @DisplayName("SQUARE_8 counts corner contacts as joins and as connections")
    void eightNeighbours() {
        final var square8 = Set.of(Lattice.SQUARE_8);
        final var diagonal = Descriptor.of(grid("x../.x./...", 3), 3, 3, square8);
        assertEquals(Lattice.SQUARE_8, diagonal.lattice());
        assertEquals(1, diagonal.joins(), "the corner touch is a join");
        assertEquals(0, Descriptor.of(grid("x../.x./...", 3), 3, 3).joins(), "and is not one on SQUARE_4");
        final var tromino = Descriptor.of(grid("xx./x../...", 3), 3, 3, square8);
        assertEquals(Kind.SQUARE, tromino.kind());
        assertEquals(3, tromino.joins(), "two edges and one corner");

        final var pieces = Primitive.BATTERY.stream().filter(p -> p.name().equals("pieces")).findFirst().orElseThrow();
        assertEquals(1, pieces.value(diagonal.canonical(), diagonal.lattice()));
        final var four = Descriptor.of(grid("x../.x./...", 3), 3, 3, Set.of(Lattice.SQUARE_4));
        assertEquals(2, pieces.value(four.canonical(), four.lattice()));

        // the keys stay pose-free with the larger neighbourhood, and hex still coexists with it
        final var both = Set.of(Lattice.SQUARE_8, Lattice.HEX_6);
        assertEquals(Kind.HEX, Descriptor.of(grid(".xx/xxx/xx.", 3), 3, 3, both).kind());
        for (var mask = 1; mask < 512; mask++) {
            final var g = new int[9];
            for (var i = 0; i < 9; i++) g[i] = (mask >> i) & 1;
            for (final var set : List.of(square8, both)) {
                final var key = Descriptor.of(g, 3, 3, set).shapeKey();
                for (var t = 0; t < 8; t++)
                    assertEquals(key, Descriptor.of(frameTransform(g, 3, 3, t), 3, 3, set).shapeKey(), mask + " " + set + " " + t);
            }
        }

        final var router = new Plexus(3, 3, Lattice.SQUARE_8);
        router.accept(grid("x../.x./...", 3), 1);
        router.accept(grid("x.x/.../...", 3), 0);
        final var other = router.read(grid(".x./x../...", 3));
        assertEquals(Verdict.ASSUMED, other.verdict(), "the other diagonal: same shape, unseen placement");
        assertEquals(1, other.label());
    }

    @Test
    @DisplayName("a lattice set needs exactly one square lattice; hex is optional")
    void latticeSets() {
        assertEquals(Lattice.DEFAULT, new Plexus(3, 3).lattices());
        assertEquals(Set.of(Lattice.SQUARE_8, Lattice.HEX_6), new Plexus(3, 3, Lattice.SQUARE_8, Lattice.HEX_6).lattices());
        for (final var bad : List.<Set<Lattice>>of(Set.of(), Set.of(Lattice.HEX_6), Set.of(Lattice.SQUARE_4, Lattice.SQUARE_8),
                Set.of(Lattice.SQUARE_4, Lattice.SQUARE_8, Lattice.HEX_6)))
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                    () -> new Plexus(3, 3, bad, true), bad.toString());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new Plexus(3, 3, new Lattice[0]));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new Plexus(3, 3, Lattice.SQUARE_4, Lattice.SQUARE_4));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Descriptor.of(new int[9], 3, 3, Set.of(Lattice.HEX_6)));
    }

    @Test
    @DisplayName("the upright and the side T share everything but the orientation")
    void sideT() {
        final var up = describe("xxx/.x./...", 3);
        final var side = describe("x../xx./x..", 3);
        assertEquals(up.shapeKey(), side.shapeKey());
        assertEquals(List.of("short-axis mirror"), up.symmetries());
        assertEquals(0, up.orientation());
        assertEquals(1, side.orientation());
        assertEquals(4, up.mass());
        assertEquals(3, up.joins());
    }

    // ---- companion planes ------------------------------------------------------------------------------

    @Test
    @DisplayName("toCanonical: turning, mirroring or moving the frame leaves the fold unchanged, on every lattice set")
    void foldsArePoseAndPlaceFree() {
        final var rng = new java.util.Random(7);
        for (final var lattices : List.of(Lattice.DEFAULT, Set.of(Lattice.SQUARE_8), Set.of(Lattice.SQUARE_4)))
            for (var mask = 1; mask < 512; mask++) {
                final var g = new int[9];
                for (var i = 0; i < 9; i++) g[i] = (mask >> i) & 1;
                for (var trial = 0; trial < 6; trial++) {
                    final var c = new int[9];
                    for (var i = 0; i < 9; i++) c[i] = rng.nextInt(3) == 0 ? 1 : 0;
                    final var fold = Descriptor.of(g, 3, 3, lattices).toCanonical(g, c);
                    for (var t = 1; t < 8; t++) {
                        final var tg = frameTransform(g, 3, 3, t);
                        final var tc = frameTransform(c, 3, 3, t);
                        assertEquals(fold, Descriptor.of(tg, 3, 3, lattices).toCanonical(tg, tc),
                                mask + " " + Arrays.toString(c) + " " + lattices + " t" + t);
                    }
                    // the same pair moved together inside a larger frame
                    final var mg = place5(g, 1, 2);
                    final var mc = place5(c, 1, 2);
                    assertEquals(fold, Descriptor.of(mg, 5, 5, lattices).toCanonical(mg, mc), "moved " + mask);
                }
            }
    }

    @Test
    @DisplayName("toCanonical keeps where the companions sit relative to the shape")
    void foldsKeepRelativePlacement() {
        final var dot = grid(".../.x./...", 3);
        final var beside = grid(".../..x/...", 3);
        final var corner = grid(".../.../..x", 3);
        final var d = Descriptor.of(dot, 3, 3);
        assertNotEquals(d.toCanonical(dot, beside), d.toCanonical(dot, corner), "edge neighbour vs corner neighbour");
        // a dot is symmetric under all of D4, so the companion's side does not matter, only its distance
        assertEquals(d.toCanonical(dot, beside), d.toCanonical(dot, grid(".x./.../...", 3)));
        // the reference itself lands on its canonical pose, starting at (0, 0)
        final var l = grid("x../x../xx.", 3);
        final var ld = Descriptor.of(l, 3, 3);
        final var cells = ld.toCanonical(l).cells(0);
        assertEquals(4, cells.length);
        assertEquals(0, Arrays.stream(cells).mapToInt(x -> x[0]).min().orElseThrow());
        assertEquals(0, Arrays.stream(cells).mapToInt(x -> x[1]).min().orElseThrow());
        // companions outside the reference box keep their offset, negative coordinates included
        final var left = grid(".../x.x/...", 3);
        final var onRight = Descriptor.of(grid(".../..x/...", 3), 3, 3).toCanonical(grid(".../..x/...", 3), left);
        assertTrue(Arrays.stream(onRight.cells(1)).anyMatch(x -> x[0] < 0 || x[1] < 0), onRight.toString());
    }

    @Test
    @DisplayName("toCanonical: an empty shape folds nothing, and a plane of another frame is refused")
    void foldEdges() {
        final var empty = new int[9];
        final var c = grid("x../.../...", 3);
        final var fold = Descriptor.of(empty, 3, 3).toCanonical(empty, c);
        assertEquals(0, fold.cells(0).length);
        assertEquals(List.of(0, 0), Arrays.stream(fold.cells(1)[0]).boxed().toList(), "unmoved, input coordinates");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Descriptor.of(c, 3, 3).toCanonical(new int[16]));
    }

    /** A 3x3 grid placed with its frame's top-left at {@code (row, col)} of a 5x5 frame. */
    private static int[] place5(int[] g, int row, int col) {
        final var out = new int[25];
        for (var i = 0; i < 9; i++) out[(row + i / 3) * 5 + col + i % 3] = g[i];
        return out;
    }

    /** Every turn and mirror of the frame leaves the shape part of the key unchanged. */
    @ParameterizedTest(name = "shape keys are pose-free — {0}")
    @MethodSource("datasets")
    void shapeKeysArePoseFree(String stem) {
        final var d = Fixtures.load(stem);
        for (final var grid : d.grids()) {
            final var key = Descriptor.of(grid, d.rows(), d.cols()).shapeKey();
            for (var t = 0; t < 8; t++) {
                if (!d.square() && (t == 1 || t == 3 || t == 6 || t == 7)) continue;  // these swap a rectangle's sides
                assertEquals(key, Descriptor.of(frameTransform(grid, d.rows(), d.cols(), t), d.rows(), d.cols()).shapeKey(),
                        stem + ": transform " + t);
            }
        }
    }

    // ---- training and reading ------------------------------------------------------------------------

    @ParameterizedTest(name = "every training grid reads back its own label (self-extension {0})")
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void trainingIsReproduced(boolean extend) {
        for (final var stem : Fixtures.ALL) {
            final var d = Fixtures.load(stem);
            final var router = new Plexus(d.rows(), d.cols(), Lattice.DEFAULT, extend);
            router.fit(d.grids(), d.labels());
            final Map<String, Set<Integer>> labels = new HashMap<>();
            for (var i = 0; i < d.grids().size(); i++)
                labels.computeIfAbsent(Arrays.toString(d.grids().get(i)), k -> new HashSet<>()).add(d.labels().get(i));
            for (var i = 0; i < d.grids().size(); i++) {
                if (labels.get(Arrays.toString(d.grids().get(i))).size() > 1) continue;
                final var a = router.read(d.grids().get(i), Extrapolate.NONE);
                assertTrue(a.verdict() == Verdict.POSITIVE || a.verdict() == Verdict.NEGATIVE, stem + " grid " + i);
                assertEquals((int) d.labels().get(i), a.label(), stem + " grid " + i);
            }
        }
    }

    @Test
    @DisplayName("an unseen orientation is ASSUMED from the pure prefix, until training says otherwise")
    void orientationIsAssumedThenLearned() {
        final var router = new Plexus(3, 3);
        router.accept(grid("xxx/.x./...", 3), 1);
        router.accept(grid(".../xxx/.x.", 3), 1);
        final var side = grid("x../xx./x..", 3);
        assertEquals(Verdict.UNSEEN, router.read(side, Extrapolate.NONE).verdict());
        final var a = router.read(side, Extrapolate.PLACEMENT);
        assertEquals(Verdict.ASSUMED, a.verdict());
        assertEquals(1, a.label());
        assertEquals(Verdict.ASSUMED, router.read(side).verdict(), "the default setting folds orientation too");

        router.accept(side, 0);
        assertEquals(Verdict.NEGATIVE, router.read(side).verdict(), "the side T now has its own label");
        assertEquals(Verdict.POSITIVE, router.read(grid("xxx/.x./...", 3)).verdict());
        final var otherSide = router.read(grid(".x./.xx/.x.", 3), Extrapolate.PLACEMENT);
        assertEquals(0, otherSide.label(), "another side T follows the side-T branch");
    }

    @Test
    @DisplayName("the default setting folds an unseen symmetry class only at the same mass and joins")
    void symmetryFoldNeverCrossesMass() {
        final var router = new Plexus(3, 3);
        router.accept(grid("xxx/.x./...", 3), 1);                          // T: mass 4, joins 3
        final var l = grid("xxx/x../...", 3);                              // L: mass 4, joins 3, no symmetry
        assertEquals(Verdict.ASSUMED, router.read(l).verdict(), "same box, mass and joins: folded over symmetry");
        assertEquals(Verdict.UNSEEN, router.read(l, Extrapolate.PLACEMENT).verdict());

        final var u = grid("x.x/xxx/...", 3);                              // U: same symmetry as the T, mass 5
        assertEquals(Verdict.UNSEEN, router.read(u).verdict(), "a different mass is never guessed at the default");
        assertEquals(Verdict.ASSUMED, router.read(u, Extrapolate.EVERYTHING).verdict());

        final var bar = grid("xxx/.../...", 3);                            // a different box
        assertEquals(Verdict.UNSEEN, router.read(bar, Extrapolate.EVERYTHING).verdict(), "the root never stands in");
    }

    @Test
    @DisplayName("the flipped T in the twin data reads back 0, separated without memorising a twin")
    void twinIsSeparated() {
        final var d = Fixtures.load("twin-t-flip-3x3");
        final var router = new Plexus(d.rows(), d.cols());
        router.fit(d.grids(), d.labels());
        for (var i = 0; i < d.grids().size(); i++)
            assertEquals((int) d.labels().get(i), router.read(d.grids().get(i)).label());
    }

    // ---- self-extension ------------------------------------------------------------------------------

    @Test
    @DisplayName("different shapes with one key get a shape coordinate; a moved copy then reads correctly")
    void extensionSeparatesShapes() {
        final var pair = collidingShapes(4);
        final var a = pair.get(0);
        final var b = pair.get(1);
        final var router = new Plexus(6, 6);
        router.accept(place(a, 0, 0, 6), 1);
        router.accept(place(b, 0, 0, 6), 0);
        assertFalse(router.extensions().isEmpty(), "a shape coordinate was inserted");
        assertEquals(1, router.read(place(a, 1, 1, 6)).label(), "shape A moved");
        assertEquals(0, router.read(place(b, 1, 1, 6)).label(), "shape B moved");

        final var plain = new Plexus(6, 6, Lattice.DEFAULT, false);
        plain.accept(place(a, 0, 0, 6), 1);
        plain.accept(place(b, 0, 0, 6), 0);
        assertTrue(plain.extensions().isEmpty());
    }

    @Test
    @DisplayName("one shape, two labels by placement: no extension, the corner separates them")
    void placementCollisionNeedsNoExtension() {
        final var router = new Plexus(3, 3);
        router.accept(grid("x../.../...", 3), 1);
        router.accept(grid(".../.x./...", 3), 0);
        assertTrue(router.extensions().isEmpty());
        assertEquals(1, router.read(grid("x../.../...", 3)).label());
        assertEquals(0, router.read(grid(".../.x./...", 3)).label());
    }

    @Test
    @DisplayName("diagnose names what resolves each mixed node")
    void diagnose() {
        final var d = Fixtures.load("separable-path-fork-5x5");
        assertTrue(d.trained().diagnose().contains("several shapes: separated by max-degree"));
        final var dots = new Plexus(3, 3);
        dots.accept(grid("x../.../...", 3), 1);
        dots.accept(grid(".../.x./...", 3), 0);
        assertTrue(dots.diagnose().contains("one shape: separated by orientation, pose or corner"));
        assertTrue(Fixtures.load("conservation-t-orbit-3x3").trained().diagnose().contains("none: every node is pure"));

        final var twice = new Plexus(3, 3);
        twice.accept(grid("xxx/.x./...", 3), 1);
        twice.accept(grid("xxx/.x./...", 3), 0);
        assertTrue(twice.diagnose().contains("the same grid trained with both labels"));

        // two different shapes that agree on every level down to the corner, and that no primitive separates
        final var a = grid("xx.x/x.../..../....", 4);
        final var b = grid("xx.x/.x../..../....", 4);
        final var shared = new Plexus(4, 4);
        shared.accept(a, 1);
        shared.accept(b, 0);
        final var report = shared.diagnose();
        assertTrue(report.contains("several shapes: no primitive separates them"), report);
        assertTrue(report.contains("different shapes with one full key"), report);
        assertFalse(report.contains("the same grid trained with both labels"), report);
        assertEquals(Verdict.POSITIVE, shared.read(a).verdict(), "each stored grid still answers with its own label");
        assertEquals(Verdict.NEGATIVE, shared.read(b).verdict());
    }

    // ---- slices --------------------------------------------------------------------------------------

    @Test
    @DisplayName("a symmetry slice returns the trained T's, and an exact one that excludes them returns nothing")
    void symmetrySlice() {
        final var d = Fixtures.load("conservation-t-orbit-3x3");
        final var router = new Plexus(3, 3);
        router.fit(d.grids(), d.labels());
        final var got = new ArrayList<int[]>();
        final var r = router.slice(Slice.on(Root.D4).atLeast("short-axis mirror").label(1),
                (g, ctx, ev) -> { if (ev == Slice.Evidence.CONFIRMED) got.add(g); });
        assertEquals(8, r.confirmed());
        assertEquals(8, got.size());
        assertEquals(0, router.slice(Slice.on(Root.D4).exactly("rot180").label(1), (g, c, e) -> { }).confirmed());
        assertEquals(0, router.slice(Slice.on(Root.D6), (g, c, e) -> { }).confirmed(), "nothing went hex");
    }

    @Test
    @DisplayName("symmetry names are matched loosely, and an unknown one is refused with the list")
    void symmetryNames() {
        assertEquals("long-axis mirror", Slice.symmetryName("long-axis-mirror"));
        assertEquals("long-axis mirror", Slice.symmetryName(" Long Axis Mirror "));
        assertEquals("mirrorH", Slice.symmetryName("mirrorh"));
        assertTrue(Slice.symmetryNames().containsAll(List.of("rot180", "short-axis mirror", "rot90", "diagAnti", "rot60", "mirror150")));
        final var e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Slice.on(Root.D4).atLeast("sideways mirror"));
        assertTrue(e.getMessage().contains("short-axis mirror"), e.getMessage());

        final var d = Fixtures.load("conservation-t-orbit-3x3");
        final var hyphenated = d.trained().slice(Slice.on(Root.D4).atLeast("short-axis-mirror").label(1), (g, c, ev) -> { });
        assertEquals(8, hyphenated.confirmed(), "the hyphenated spelling finds the T's");
    }

    @Test
    @DisplayName("a slice can fold placements into poses or shapes, and constrain orientation, pose and corner")
    void perPoseAndPerShape() {
        final var d = Fixtures.load("conservation-t-orbit-3x3");
        final var router = d.trained();
        final var none = Extrapolate.NONE;

        assertEquals(8, router.slice(Slice.on(Root.BOTH).label(1).extrapolate(none), (g, c, e) -> { }).confirmed());
        assertEquals(4, router.slice(Slice.on(Root.BOTH).label(1).extrapolate(none).per(Slice.Per.POSE), (g, c, e) -> { }).confirmed(),
                "stem down, stem up, and the two sideways T's");

        final var shapes = new ArrayList<int[]>();
        final var r = router.slice(Slice.on(Root.BOTH).label(1).extrapolate(none).per(Slice.Per.SHAPE), (g, c, e) -> shapes.add(g));
        assertEquals(1, r.confirmed(), "one shape");
        assertTrue(Arrays.equals(grid("xxx/.x./...", 3), shapes.get(0)), "shown in its canonical pose, top-left");

        assertEquals(2, router.slice(Slice.on(Root.D4).orientation(0).pose(0).extrapolate(none), (g, c, e) -> { }).confirmed(),
                "the canonical pose, at its two trained positions");
        final var one = new ArrayList<int[]>();
        router.slice(Slice.on(Root.D4).orientation(0).pose(0).corner(1, 0).extrapolate(none), (g, c, e) -> one.add(g));
        assertEquals(1, one.size());
        assertTrue(Arrays.equals(grid(".../xxx/.x.", 3), one.get(0)));
        assertEquals(4, router.slice(Slice.on(Root.D4).orientation(1).extrapolate(none), (g, c, e) -> { }).confirmed(), "the side T's");

        // folding also applies to what is assumed: the L's and S's the default setting adds are 2 more shapes
        final var withAssumed = router.slice(Slice.on(Root.BOTH).per(Slice.Per.SHAPE), (g, c, e) -> { });
        assertEquals(1, withAssumed.confirmed());
        assertEquals(2, withAssumed.assumed());
    }

    /** What a slice generates is exactly what reading answers for: no grid missed, none extra. */
    @ParameterizedTest(name = "slice generation agrees with reading under {0}")
    @EnumSource(Extrapolate.class)
    void generationAgreesWithReading(Extrapolate extrapolate) {
        for (final var stem : List.of("conservation-t-orbit-3x3", "twin-t-flip-3x3", "position-corner-dot-3x3")) {
            final var d = Fixtures.load(stem);
            final var router = new Plexus(3, 3);
            router.fit(d.grids(), d.labels());
            for (final int label : new int[]{0, 1}) {
                final var sliced = new HashSet<String>();
                router.slice(Slice.on(Root.BOTH).label(label).extrapolate(extrapolate), (g, c, e) -> sliced.add(Arrays.toString(g)));
                final var read = new HashSet<String>();
                for (var mask = 0; mask < 512; mask++) {
                    final var g = new int[9];
                    for (var i = 0; i < 9; i++) g[i] = (mask >> i) & 1;
                    final var a = router.read(g, extrapolate);
                    if (a.verdict() != Verdict.UNSEEN && a.label() == label) read.add(Arrays.toString(g));
                }
                assertEquals(read, sliced, stem + " label " + label);
            }
        }
    }

    @Test
    @DisplayName("the callback receives the controlling context and the evidence")
    void callbackContext() {
        final var router = new Plexus(3, 3);
        router.accept(grid("xxx/.x./...", 3), 1);
        final var assumedDepths = new ArrayList<Integer>();
        router.slice(Slice.on(Root.D4).box(2, 3).mass(4, 4).joins(3, 3).extrapolate(Extrapolate.PLACEMENT), (g, ctx, ev) -> {
            assertEquals(1, ctx.answer());
            assertTrue(ctx.pure());
            assertEquals("box", ctx.path().get(0).coordinate());
            if (ev == Slice.Evidence.ASSUMED) assumedDepths.add(ctx.depth());
        });
        assertFalse(assumedDepths.isEmpty(), "unseen poses and placements of the T are generated");
        for (var i = 1; i < assumedDepths.size(); i++)
            assertTrue(assumedDepths.get(i - 1) >= assumedDepths.get(i), "deeper stand-ins first");
    }

    // ---- helpers -------------------------------------------------------------------------------------

    /**
     * One of the eight turns and mirrors of the whole frame: 0 identity, 1 rot90, 2 rot180, 3 rot270, 4 flip rows,
     * 5 flip columns, 6 transpose, 7 anti-transpose. 1, 3, 6 and 7 need a square frame.
     */
    private static int[] frameTransform(int[] g, int rows, int cols, int t) {
        final var out = new int[g.length];
        for (var r = 0; r < rows; r++)
            for (var c = 0; c < cols; c++) {
                final int[] to = switch (t) {
                    case 0 -> new int[]{r, c};
                    case 1 -> new int[]{c, rows - 1 - r};
                    case 2 -> new int[]{rows - 1 - r, cols - 1 - c};
                    case 3 -> new int[]{cols - 1 - c, r};
                    case 4 -> new int[]{rows - 1 - r, c};
                    case 5 -> new int[]{r, cols - 1 - c};
                    case 6 -> new int[]{c, r};
                    default -> new int[]{cols - 1 - c, rows - 1 - r};
                };
                out[to[0] * cols + to[1]] = g[r * cols + c];
            }
        return out;
    }

    private static Descriptor describe(String rows, int side) { return Descriptor.of(grid(rows, side), side, side); }

    private static int[] grid(String rows, int side) {
        final var parts = rows.split("/");
        final var g = new int[side * side];
        for (var r = 0; r < parts.length; r++)
            for (var c = 0; c < parts[r].length(); c++) if (parts[r].charAt(c) == 'x') g[r * side + c] = 1;
        return g;
    }

    /** Two different shapes in a {@code side×side} frame with the same shape key (search, not hand-picked). */
    private static List<int[]> collidingShapes(int side) {
        final Map<String, List<int[]>> byKey = new HashMap<>();
        for (var mask = 1; mask < (1 << (side * side)); mask++) {
            final var g = new int[side * side];
            for (var i = 0; i < g.length; i++) g[i] = (mask >> i) & 1;
            if (g[0] == 0) continue;                                   // corner-anchored, to keep it small
            final var desc = Descriptor.of(g, side, side);
            final var list = byKey.computeIfAbsent(desc.shapeKey(), k -> new ArrayList<>());
            for (final var other : list)
                if (!Descriptor.of(other, side, side).sameShape(desc)) return List.of(other, crop(g, side));
            if (list.isEmpty()) list.add(crop(g, side));
        }
        throw new AssertionError("no colliding pair found");
    }

    /** A copy of the grid. The search only keeps grids with cell 0 filled, so their box already starts at the top-left. */
    private static int[] crop(int[] g, int side) { return g.clone(); }

    private static int[] place(int[] small, int row, int col, int frame) {
        final var side = (int) Math.round(Math.sqrt(small.length));
        int minR = side, minC = side;
        for (var i = 0; i < small.length; i++) if (small[i] == 1) { minR = Math.min(minR, i / side); minC = Math.min(minC, i % side); }
        final var g = new int[frame * frame];
        for (var i = 0; i < small.length; i++)
            if (small[i] == 1) g[(row + i / side - minR) * frame + col + i % side - minC] = 1;
        return g;
    }
}
