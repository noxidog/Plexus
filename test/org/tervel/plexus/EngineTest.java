package org.tervel.plexus;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The engine: lazy grids, templates matched by containment in every pose, expressions, stages. */
final class EngineTest {

    private static final Vocabulary WORDS = new Vocabulary(
            Map.of("digit", Set.of("1", "2"), "shut", Set.of("#", "F")),
            Map.of("1", 1, "2", 2),
            Set.of("edge", "any"),
            Set.of("any"),
            Set.of("flag", "seen"));

    /** A grid over {@code rows} (one label per character), counting how often the oracle is asked. */
    private static Grid grid(int[] asked, String... rows) {
        return Grid.of(rows.length, rows[0].length(), WORDS, (r, c) -> {
            asked[0]++;
            return String.valueOf(rows[r].charAt(c));
        });
    }

    // ---- grid -------------------------------------------------------------------------------------

    @Test
    void aGridAsksForACellOnlyWhenSomethingNeedsItAndOnlyOnce() {
        final var asked = new int[1];
        final var g = grid(asked, "1#", "#2");
        assertEquals(0, asked[0], "nothing is computed up front");
        assertTrue(g.in("any", 0, 0), "an everywhere plane answers without a label");
        assertEquals(0, asked[0]);
        assertTrue(g.in("digit", 0, 0));
        assertTrue(g.in("1", 0, 0));
        assertEquals(1, asked[0], "the second question about the cell reuses the first answer");
        final var derived = g.mark("click", 1, 0, "rule").relabel(0, 1, "flag");
        assertTrue(derived.in("shut", 1, 0));
        assertEquals(2, asked[0]);
        assertTrue(g.in("shut", 1, 0), "a grid derived from this one shares what it learned");
        assertEquals(2, asked[0]);
        assertEquals(2, g.recognized());
    }

    @Test
    void aGridIsChangedOnlyByReturningANewOne() {
        final var g = grid(new int[1], "1#", "#2");
        final var marked = g.mark("click", 0, 1, "freed");
        assertFalse(g.marked("click", 0, 1));
        assertTrue(marked.marked("click", 0, 1));
        assertEquals("freed", marked.note("click", 0, 1).orElseThrow());
        assertSame(marked, marked.mark("click", 0, 1, "later"), "a cell keeps its first note");
        final var flagged = marked.relabel(0, 1, "flag");
        assertEquals("#", marked.label(0, 1));
        assertEquals("flag", flagged.label(0, 1));
        assertEquals("#", flagged.read(0, 1), "what was read stays what was read");
        assertTrue(flagged.written(0, 1));
        assertFalse(flagged.written(1, 0));
        assertThrows(IllegalArgumentException.class, () -> g.relabel(0, 0, "2"),
                "a read label is never written: a conclusion cannot pass for an observation");
        final var lying = Grid.of(1, 1, WORDS, (r, c) -> "flag");
        assertThrows(IllegalStateException.class, () -> lying.label(0, 0), "an oracle never answers with a state");
        assertFalse(flagged.in("shut", 0, 1), "a relabelled cell answers to its new label");
        assertTrue(flagged.marked("click", 0, 1));
        assertEquals(2, g.value(1, 1).orElseThrow());
        assertTrue(g.value(0, 1).isEmpty());
        assertTrue(g.in("edge", -1, 0), "off the grid only the outside planes hold");
        assertFalse(g.in("shut", -1, 0));
        assertEquals(List.of("0,1", "1,0"), g.cells("shut").stream().map(rc -> rc[0] + "," + rc[1]).toList());
    }

    // ---- template ---------------------------------------------------------------------------------

    private static final String[][] ELL = {{"1", "#"}, {null, "#"}};          // anchor at (0,0)

    @Test
    void anAsymmetricTemplateHasEightPosesAndAnyDeclaredRotationIsTheSameTemplate() {
        final var t = Template.of(ELL, 0, 0);
        assertEquals(8, t.poses().size());
        assertEquals("identity", t.poses().get(0).move());
        final var turned = Template.of(new String[][]{{null, "1"}, {"#", "#"}}, 0, 1);
        assertEquals(t, turned, "a rotation of a template is the same template");
        assertEquals(t.canonical().cells(), turned.canonical().cells());
        assertArrayEquals2(new int[]{2, 2}, t.box());

        final var plus = Template.of(new String[][]{{null, "#", null}, {"#", "#", "#"}, {null, "#", null}}, 1, 1);
        assertEquals(1, plus.poses().size(), "a template fixed by every motion has one pose");
        final var bar = Template.of(new String[][]{{"#", "1", "#"}}, 0, 1);
        assertEquals(2, bar.poses().size(), "a bar about its middle: lying or standing");
    }

    @Test
    void aTemplateHoldsByContainmentInAnyPose() {
        // The L: a 1, the cell right of it, and the cell below that one.
        final var t = Template.of(ELL, 0, 0);
        assertTrue(t.matches(grid(new int[1], "1#", "2#"), 0, 0), "the declared pose");
        assertTrue(t.matches(grid(new int[1], "#1", "#2"), 0, 1), "its mirror");
        assertTrue(t.matches(grid(new int[1], "#2", "#1"), 1, 1), "its half turn: left of the 1, then up");
        assertTrue(t.matches(grid(new int[1], "1##", "##2"), 0, 0),
                "extra cells the template does not name are not looked at");
        assertFalse(t.matches(grid(new int[1], "12", "2#"), 0, 0), "a cell it names is missing in every pose");
    }

    @Test
    void aTemplateCellMayLieOffTheGridWhereTheVocabularySaysTheEdgeFits() {
        final var walled = Template.of(new String[][]{{"edge"}, {"1"}}, 1, 0);
        final var g = grid(new int[1], "1#", "##");
        assertTrue(walled.matches(g, 0, 0), "above the top row is `edge`");
        assertFalse(walled.matches(grid(new int[1], "###", "#1#", "###"), 1, 1),
                "with every side on the grid no pose finds an edge");
    }

    @Test
    void placementsCoveringACellCountWhatTheyContain() {
        // A digit with its eight neighbours, counted as `any`.
        final var around = Template.of(new String[][]{{"any", "any", "any"}, {"any", "digit", "any"},
                {"any", "any", "any"}}, 1, 1);
        assertEquals(1, around.poses().size());
        final var g = grid(new int[1], "#2#", "F1#", "###");
        final var at = around.covering(g, 0, 0, "any");
        assertEquals(List.of("0,1", "1,1"), at.stream().map(p -> p.row() + "," + p.col()).sorted().toList(),
                "the 2 and the 1 both have (0,0) among their neighbours");
        final var one = at.stream().filter(p -> p.row() == 1).findFirst().orElseThrow();
        assertEquals(7, one.count(g, "any", "shut"), "the 1 sees seven shut cells: six covered and the flag F");
        assertEquals(1, one.count(g, "any", "F"));
        assertEquals(1, one.at("digit").size());
    }

    // ---- expression, stages, frames, descriptor counts --------------------------------------------

    @Test
    void expressionsAddAndSubtractNamedNumbers() {
        final var e = Expression.parse("value - flag + 1");
        assertEquals(Set.of("value", "flag"), e.names());
        assertEquals(3, e.eval(n -> n.equals("value") ? 3 : 1));
        assertEquals(-2, Expression.parse("-2").eval(n -> 0));
        assertThrows(IllegalArgumentException.class, () -> Expression.parse("value -"));
        assertThrows(IllegalArgumentException.class, () -> Expression.parse("value * 2"));
    }

    @Test
    void aStageReadsTheGridItWasGivenAndAPipelineFoldsInOrder() {
        // Mark every shut cell next to a marked one: reading the input means the marks do not spread
        // further within one stage, however the cells are visited.
        final var g = grid(new int[1], "####").mark("seen", 0, 0, "");
        final Stage<String> spread = Stage.atEach("shut", (ctx, in, r, c) -> in.marked("seen", r, c - 1),
                (ctx, out, r, c) -> out.mark("seen", r, c, ctx));
        final var once = spread.apply("ctx", g);
        assertEquals(2, once.cells("seen").size(), "one step, not a cascade");
        final var twice = Stage.of(List.of(spread, spread)).apply("ctx", g);
        assertEquals(3, twice.cells("seen").size(), "the second stage sees the first one's output");
        assertSame(g, spread.onlyIf((ctx, x) -> false).apply("ctx", g));
        assertEquals(java.util.Optional.of("ctx"), once.note("seen", 0, 1), "every callback is handed the context");

        // Written through, the same rule spreads along the whole row in one sweep: each cell sees the
        // mark the one before it just wrote.
        final Stage<String> through = Stage.sweep("shut", (ctx, out, r, c) -> out.marked("seen", r, c - 1),
                (ctx, out, r, c) -> out.mark("seen", r, c, ""));
        assertEquals(4, through.apply("ctx", g).cells("seen").size());
        // A cell an earlier write took out of the anchor plane is not visited.
        final Stage<String> relabel = Stage.sweep("shut", (ctx, out, r, c) -> true,
                (ctx, out, r, c) -> out.relabel(r, c + 1 < 4 ? c + 1 : c, "seen"));
        final var swept = relabel.apply("ctx", grid(new int[1], "##"));
        assertEquals("seen", swept.label(0, 1));
        assertEquals("#", swept.read(0, 1));
    }

    @Test
    void aFrameSaysWhichMotionsLeaveItUnchanged() {
        final int[] oneTwoOne = {1, 2, 1, 0, 0, 0, 0, 0, 0};
        final var moves = Frame.moves(3, 3);
        assertEquals(8, moves.size());
        final var mask = Frame.stabilizer(oneTwoOne, 3, 3);
        assertEquals(2, Integer.bitCount(mask));
        assertTrue((mask & (1 << moves.indexOf("mirrorV"))) != 0);
        assertEquals(4, Frame.moves(2, 3).size(), "a rectangle admits no transposing move");
        assertEquals(List.of(3, 2, 1, 0, 0, 0, 0, 0, 0).toString(),
                java.util.Arrays.toString(Frame.apply("mirrorV", new int[]{1, 2, 3, 0, 0, 0, 0, 0, 0}, 3, 3)));
    }

    @Test
    void aDescriptorCountsItsShapesOwnSymmetry() {
        final var domino = Descriptor.of(new int[]{1, 1, 0, 0}, 2, 2, Set.of(Lattice.SQUARE_8));
        assertEquals(4, domino.stabilizerSize());
        assertEquals(2, domino.orbitSize(), "lying or standing");
        final var ell = Descriptor.of(new int[]{1, 0, 1, 1}, 2, 2, Set.of(Lattice.SQUARE_8));
        assertEquals(2, ell.stabilizerSize());
        assertEquals(4, ell.orbitSize());
        final var dot = Descriptor.of(new int[]{1, 0, 0, 0}, 2, 2, Set.of(Lattice.SQUARE_8));
        assertEquals(1, dot.orbitSize());
        final var notEqual = Template.of(new String[][]{{"1", "2"}}, 0, 0);
        assertNotEquals(notEqual, Template.of(new String[][]{{"2", "1"}}, 0, 0), "a different anchor is a different template");
    }

    private static void assertArrayEquals2(int[] want, int[] got) {
        final var w = new ArrayList<Integer>();
        for (final var x : want) w.add(x);
        final var g = new ArrayList<Integer>();
        for (final var x : got) g.add(x);
        assertEquals(w, g);
    }
}
