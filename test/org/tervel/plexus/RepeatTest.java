package org.tervel.plexus;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Repeat}: how a grid of cells repeats, exactly; and {@link Periods}: where, what it repeats, and what breaks
 * it.
 */
final class RepeatTest {

    /** {@code tile} stamped over a {@code rows × cols} frame, its corner at {@code (x0, y0)}. */
    private static int[] tiled(int rows, int cols, int[][] tile, int x0, int y0) {
        final var th = tile.length;
        final var tw = tile[0].length;
        final var g = new int[rows * cols];
        for (var y = 0; y < rows; y++)
            for (var x = 0; x < cols; x++)
                g[y * cols + x] = tile[Math.floorMod(y - y0, th)][Math.floorMod(x - x0, tw)];
        return g;
    }

    /** A {@code w × h} tile: 1 inside a one-cell ring of 0, with a 0 dot at its centre. */
    private static int[][] tile(int w, int h) {
        final var t = new int[h][w];
        for (var y = 1; y < h - 1; y++) for (var x = 1; x < w - 1; x++) t[y][x] = 1;
        t[h / 2][w / 2] = 0;
        return t;
    }

    @Test
    void aSquareGridRepeatsAtExactlyItsCell() {
        final var r = Repeat.of(tiled(40, 40, tile(5, 5), 2, 3), 40, 40);
        assertEquals(Repeat.Type.SQUARE, r.type());
        assertEquals(new Repeat.Step(5, 0), r.v1());
        assertEquals(new Repeat.Step(0, 5), r.v2());
        assertEquals(1.0, r.strength1());
        assertEquals(List.of("identity", "rot90", "rot180", "rot270", "mirrorH", "mirrorV", "diagMain", "diagAnti"),
                r.moves(), "the square group, in Plexus's own order");
        assertEquals(Lattice.SQUARE_4, r.lattice().orElseThrow());
    }

    @Test
    void aRectangularGridKeepsOnlyTheMovesThatKeepARectangle() {
        final var r = Repeat.of(tiled(36, 48, tile(6, 4), 0, 0), 36, 48);
        assertEquals(Repeat.Type.RECTANGULAR, r.type());
        assertEquals(new Repeat.Step(0, 4), r.v1(), "the shortest step comes first, pointing down when it stands");
        assertEquals(new Repeat.Step(6, 0), r.v2());
        assertEquals(List.of("identity", "rot180", "mirrorH", "mirrorV"), r.moves());
    }

    @Test
    void aHexLatticeIsExactWhereTheGridIsStoredOnHex() {
        // Axial storage: (q, r) = (column, row). Steps (3, 0) and (0, 3) are equal and at 60° on the hex plane.
        final int rows = 30, cols = 30;
        final var g = new int[rows * cols];
        for (var r = 0; r < rows; r++)
            for (var q = 0; q < cols; q++)
                if (q % 3 == 0 && r % 3 == 0) { g[r * cols + q] = 1; if (q + 1 < cols) g[r * cols + q + 1] = 2; }
        final var hex = Repeat.of(g, rows, cols, Lattice.HEX_6);
        assertEquals(Repeat.Type.HEX, hex.type());
        assertEquals(new Repeat.Step(3, 0), hex.v1());
        assertEquals(new Repeat.Step(0, 3), hex.v2(), "the neighbour at 60°");
        assertEquals(12, hex.moves().size());
        assertEquals(Lattice.HEX_6, hex.lattice().orElseThrow());
        assertEquals(Repeat.Type.SQUARE, Repeat.of(g, rows, cols).type(), "the same cells read on a square grid");
    }

    @Test
    void stripesRepeatOneWayAndNoiseNotAtAll() {
        // Vertical stripes also repeat one row down: a rectangular lattice, not a stripe.
        final var straight = new int[24 * 24];
        for (var y = 0; y < 24; y++) for (var x = 0; x < 24; x++) straight[y * 24 + x] = x % 6 < 2 ? 1 : 0;
        final var rect = Repeat.of(straight, 24, 24);
        assertEquals(Repeat.Type.RECTANGULAR, rect.type());
        assertEquals(new Repeat.Step(0, 1), rect.v1());
        assertEquals(new Repeat.Step(6, 0), rect.v2());

        // Rows that each repeat their own random six cells repeat along the row and nowhere else.
        final var rows = new int[64 * 24];
        final var draw = new Random(2);
        for (var y = 0; y < 64; y++) {
            final var pattern = new int[6];
            for (var k = 0; k < 6; k++) pattern[k] = draw.nextInt(2);
            pattern[0] = 1;
            for (var x = 0; x < 24; x++) rows[y * 24 + x] = pattern[x % 6];
        }
        final var s = Repeat.of(rows, 64, 24);
        assertEquals(Repeat.Type.STRIPE, s.type());
        assertEquals(new Repeat.Step(6, 0), s.v1());
        assertNull(s.v2());

        final var random = new Random(4);
        final var noise = new int[48 * 48];
        for (var k = 0; k < noise.length; k++) noise[k] = random.nextInt(2);
        assertEquals(Repeat.Type.NONE, Repeat.of(noise, 48, 48).type());

        final var blob = new int[20 * 20];
        for (var y = 8; y < 12; y++) for (var x = 7; x < 13; x++) blob[y * 20 + x] = 1;
        assertEquals(Repeat.Type.NONE, Repeat.of(blob, 20, 20).type(), "a shape overlapping itself is not a repeat");
    }

    @Test
    void aBasisIsReducedToItsShortestPairAndOrientedTheSameWayEveryTime() {
        final var r = Repeat.basis(new Repeat.Step(-5, 0), new Repeat.Step(5, -5), Lattice.SQUARE_4, 1, 1);
        assertEquals(new Repeat.Step(5, 0), r.v1(), "shortest, pointing right");
        assertEquals(new Repeat.Step(0, 5), r.v2(), "the other shortest, pointing down");
        final var tall = Repeat.basis(new Repeat.Step(0, 5), new Repeat.Step(5, 0), Lattice.SQUARE_4, 1, 1);
        assertEquals(new Repeat.Step(5, 0), tall.v1(), "equal lengths: the more horizontal comes first");
        assertThrows(IllegalArgumentException.class,
                () -> Repeat.basis(new Repeat.Step(2, 0), new Repeat.Step(4, 0), Lattice.SQUARE_4, 1, 1));
        assertEquals(Repeat.Type.SQUARE, Repeat.basis(new Repeat.Step(4, 0), new Repeat.Step(0, 5), Lattice.SQUARE_4, 1, 1)
                .as(Repeat.Type.SQUARE).type(), "a declaration wins");
    }

    @Test
    void aRepeatWithDefectsIsStillItsShortestStep() {
        final var g = tiled(40, 40, tile(5, 5), 0, 0);
        g[12 * 40 + 12] = 0;                                              // two defects
        g[27 * 40 + 31] = 0;
        final var r = Repeat.of(g, 40, 40);
        assertEquals(new Repeat.Step(5, 0), r.v1(), "not a multiple that happens to miss both defects");
        assertTrue(r.strength1() < 1.0);
    }

    /**
     * A board: {@code bc × br} tiles 8 cells apart, each 1 inside a one-cell ring, so neighbouring tiles leave a
     * two-cell gap. Set in sparse noise, with some tiles replaced.
     */
    private static int[] board(int rows, int cols, int bc, int br, int x0, int y0, List<int[]> marked, List<int[][]> marks) {
        final var random = new Random(9);
        final var g = new int[rows * cols];
        for (var k = 0; k < g.length; k++) g[k] = random.nextInt(10) < 3 ? 1 : 0;
        for (var r = 0; r < br; r++)
            for (var c = 0; c < bc; c++) {
                var t = ringed();
                for (var m = 0; m < marked.size(); m++)
                    if (marked.get(m)[0] == r && marked.get(m)[1] == c) t = marks.get(m);
                for (var y = 0; y < 8; y++)
                    for (var x = 0; x < 8; x++) g[(y0 + r * 8 + y) * cols + x0 + c * 8 + x] = t[y][x];
            }
        return g;
    }

    private static int[][] ringed() {
        final var t = new int[8][8];
        for (var y = 1; y < 7; y++) for (var x = 1; x < 7; x++) t[y][x] = 1;
        return t;
    }

    @Test
    void theRegionIsTheAllowedBlockThatStandsOutAndTheDefectsAreWhatBreaksIt() {
        final var one = ringed();
        for (var y = 2; y < 6; y++) one[y][3] = 0;                       // a stroke down the middle
        final var two = ringed();
        for (var x = 2; x < 6; x++) { two[2][x] = 0; two[5][x] = 0; }    // two bars across
        final var marked = new ArrayList<int[]>();
        final var marks = new ArrayList<int[][]>();
        for (final var rc : new int[][]{{1, 1}, {2, 4}, {4, 2}}) { marked.add(rc); marks.add(one); }
        for (final var rc : new int[][]{{0, 5}, {3, 0}}) { marked.add(rc); marks.add(two); }
        final int rows = 100, cols = 120, x0 = 29, y0 = 33;
        final var g = board(rows, cols, 9, 6, x0, y0, marked, marks);

        final var r = Repeat.of(g, rows, cols);
        assertEquals(Repeat.Type.SQUARE, r.type());
        assertEquals(new Repeat.Step(8, 0), r.v1());

        // The tile's corner and the crossing of the two-cell gaps are equally symmetric: the caller tells them
        // apart. Here by what this vocabulary knows: a tile's corner cell is part of its ring.
        final var asked = new ArrayList<List<Periods.Phase>>();
        final Periods.Picker byRing = (candidates, cells, motif, repeat) -> {
            asked.add(candidates);
            return candidates.stream().filter(c -> motif[cells.indexOf(c.corner())] == 0
                    && motif[cells.indexOf(new Repeat.Step((c.corner().x() + 1) % 8, (c.corner().y() + 1) % 8))] == 1)
                    .findFirst().orElse(null);
        };
        final var p = r.periods(g, rows, cols, List.of(new int[]{6, 6}, new int[]{9, 6}, new int[]{12, 8}), byRing);
        assertNotNull(p);
        assertEquals(2, asked.get(0).size(), "a tile's corner and the gap crossing, both fully symmetric");
        assertEquals(8, asked.get(0).get(0).held());
        assertEquals(9, p.cols());
        assertEquals(6, p.rows());
        assertEquals(new Repeat.Step(x0, y0), p.origin(), "exactly");

        for (var row = 0; row < 6; row++)
            for (var col = 0; col < 9; col++)
                assertEquals(indexOf(marked, row, col) >= 0, p.defect()[row][col], "cell r" + row + "c" + col);
        final var ones = p.group()[1][1];
        assertTrue(ones > 0);
        assertEquals(ones, p.group()[2][4], "equal defects share a group");
        assertEquals(ones, p.group()[4][2]);
        final var twos = p.group()[0][5];
        assertTrue(twos > 0 && twos != ones, "unequal ones do not");
        assertEquals(twos, p.group()[3][0]);
        assertEquals(0, p.group()[0][0]);
        assertEquals(4, p.distance()[1][1], "the stroke is four cells");

        assertEquals(2, p.cellAt(x0 + 8 * 4 + 3, y0 + 8 * 2 + 5)[0]);
        assertEquals(4, p.cellAt(x0 + 8 * 4 + 3, y0 + 8 * 2 + 5)[1]);
        assertNull(p.cellAt(2, 2), "off the region");
    }

    private static int indexOf(List<int[]> marked, int row, int col) {
        for (var k = 0; k < marked.size(); k++) if (marked.get(k)[0] == row && marked.get(k)[1] == col) return k;
        return -1;
    }

    @Test
    void withoutSizesTheRegionIsTheBlockThatRepeats() {
        final var g = board(100, 120, 9, 6, 29, 33, List.of(), List.of());
        final var r = Repeat.of(g, 100, 120);
        final Periods.Picker byRing = (candidates, cells, motif, repeat) -> candidates.stream()
                .filter(c -> motif[cells.indexOf(c.corner())] == 0).findFirst().orElse(null);
        final var p = r.periods(g, 100, 120, List.of(), byRing);
        assertEquals(9, p.cols());
        assertEquals(6, p.rows());
        assertFalse(p.defect()[0][0]);
    }

    @Test
    void aBoardReadIntoCellsRepeatsEveryCellAndItsOpenedCellsAreTheDefects() {
        // 30 x 16 cells, label 1 for covered, an opened patch of blanks (2) with digits (3, 4) along its edge.
        final int rows = 16, cols = 30;
        final var g = new int[rows * cols];
        java.util.Arrays.fill(g, 1);
        for (var y = 4; y < 10; y++) for (var x = 6; x < 15; x++) g[y * cols + x] = 2;
        for (var x = 6; x < 15; x++) { g[4 * cols + x] = 3; g[9 * cols + x] = x % 2 == 0 ? 3 : 4; }
        final var r = Repeat.of(g, rows, cols);
        assertEquals(Repeat.Type.SQUARE, r.type());
        assertEquals(new Repeat.Step(1, 0), r.v1());
        assertEquals(new Repeat.Step(0, 1), r.v2());
        final var p = r.periods(g, rows, cols, List.of(new int[]{cols, rows}), null);
        assertEquals(1, p.motif()[0], "the covered cell");
        var defects = 0;
        for (final var row : p.defect()) for (final var d : row) if (d) defects++;
        assertEquals(6 * 9, defects, "exactly the opened cells");
        assertEquals(p.group()[5][7], p.group()[6][8], "two blanks, one group");
        assertTrue(p.group()[4][7] != p.group()[5][7], "a digit is not a blank");
        assertTrue(p.group()[9][7] != p.group()[9][6], "nor another digit");
    }

    @Test
    void oneGroupIsNeverSplitAtAFalseGap() {
        assertTrue(Double.isNaN(Periods.split(new double[]{1.0, 1.1, 0.9, 1.05, 0.95})[0]), "evenly spread");
        final var two = Periods.split(new double[]{1, 1.1, 0.9, 9, 9.2, 8.9});
        assertFalse(Double.isNaN(two[0]));
        assertTrue(two[0] > 1.1 && two[0] < 8.9, "the cut falls in the gap: " + two[0]);
    }

    @Test
    void aGridOfPixelsIsReadIntoCellsFirst() {
        final var picture = new int[32 * 32];
        for (var k = 0; k < picture.length; k++) picture[k] = 1 + k % 200;
        assertThrows(IllegalArgumentException.class, () -> Repeat.of(picture, 32, 32));
    }
}
