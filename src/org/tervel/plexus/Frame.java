package org.tervel.plexus;

import java.util.List;

/**
 * The rigid motions of a fixed rows × cols frame, acting on a row-major grid of any integers: the
 * square's eight on a square frame, the rectangle's four otherwise. Unlike a {@link Descriptor}, which
 * reads a shape wherever it sits, this asks which motions leave the <b>frame</b> unchanged, so position
 * counts and cell values are compared as they are.
 */
public final class Frame {

    private Frame() { }

    private static final List<String> D4 = List.of("identity", "rot90", "rot180", "rot270",
            "mirrorH", "mirrorV", "diagMain", "diagAnti");
    private static final List<String> D2 = List.of("identity", "rot180", "mirrorH", "mirrorV");

    /** The motions this frame admits, in the order a {@link #stabilizer} mask is over. */
    public static List<String> moves(int rows, int cols) {
        return rows == cols ? D4 : D2;
    }

    /** The motions fixing {@code grid}: bit {@code k} set when move {@code k} of {@link #moves} leaves it unchanged. */
    public static int stabilizer(int[] grid, int rows, int cols) {
        if (grid.length != rows * cols)
            throw new IllegalArgumentException("grid has " + grid.length + " cells, frame is " + rows + "x" + cols);
        final var moves = moves(rows, cols);
        var mask = 0;
        for (var k = 0; k < moves.size(); k++) if (fixes(moves.get(k), grid, rows, cols)) mask |= 1 << k;
        return mask;
    }

    /** {@code grid} moved by {@code move}, a new row-major grid of the same frame. */
    public static int[] apply(String move, int[] grid, int rows, int cols) {
        final var out = new int[grid.length];
        for (var r = 0; r < rows; r++)
            for (var c = 0; c < cols; c++) out[r * cols + c] = grid[src(move, r, c, rows, cols)];
        return out;
    }

    private static boolean fixes(String move, int[] grid, int rows, int cols) {
        for (var r = 0; r < rows; r++)
            for (var c = 0; c < cols; c++) if (grid[r * cols + c] != grid[src(move, r, c, rows, cols)]) return false;
        return true;
    }

    /** The cell output cell {@code (r, c)} is read from. The four transposing moves need a square frame. */
    private static int src(String move, int r, int c, int rows, int cols) {
        return switch (move) {
            case "identity" -> r * cols + c;
            case "rot90" -> (rows - 1 - c) * cols + r;
            case "rot180" -> (rows - 1 - r) * cols + (cols - 1 - c);
            case "rot270" -> c * cols + (rows - 1 - r);
            case "mirrorH" -> (rows - 1 - r) * cols + c;
            case "mirrorV" -> r * cols + (cols - 1 - c);
            case "diagMain" -> c * cols + r;
            case "diagAnti" -> (rows - 1 - c) * cols + (rows - 1 - r);
            default -> throw new IllegalArgumentException("unknown move " + move);
        };
    }
}
