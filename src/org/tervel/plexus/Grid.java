package org.tervel.plexus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A rows × cols field of labelled cells, read <b>lazily</b> and changed <b>immutably</b>.
 *
 * <p>A cell's label comes from an {@link Oracle} the caller supplies, asked the first time anything needs
 * that cell and remembered after: a cell nothing asks about is never computed. Every grid derived from
 * this one shares that memory, so a later stage never computes a cell an earlier one already did.
 *
 * <p>A stage never changes a grid; it returns a new one. {@link #relabel} gives a cell a new label (a
 * conclusion about what the cell is), and {@link #mark} puts it in a named plane with a note (a
 * conclusion about what to do with it). Planes are resolved through the {@link Vocabulary}.
 *
 * <p>What was <b>read</b> and what was <b>written</b> stay apart: {@link #read} is always the oracle's
 * answer, {@link #label} is the current one. A relabel may only write a {@linkplain Vocabulary#states
 * state}, and an oracle answering with one is an error, so nothing concluded can pass for something seen.
 */
public final class Grid {

    /** Computes the label of one cell on demand. Asked at most once per cell per grid family. */
    @FunctionalInterface
    public interface Oracle { String label(int row, int col); }

    private final int rows, cols;
    private final Vocabulary vocabulary;
    private final Memory memory;
    private final Map<Long, String> written;              // relabelled cells
    private final Map<String, Map<Long, String>> marks;   // plane -> cell -> note

    /** The oracle's answers, shared by every grid derived from the same capture. */
    private static final class Memory {
        final Oracle oracle;
        final String[] labels;
        int asked;

        Memory(Oracle oracle, int size) { this.oracle = oracle; this.labels = new String[size]; }
    }

    private Grid(int rows, int cols, Vocabulary vocabulary, Memory memory, Map<Long, String> written,
                 Map<String, Map<Long, String>> marks) {
        this.rows = rows; this.cols = cols; this.vocabulary = vocabulary; this.memory = memory;
        this.written = written; this.marks = marks;
    }

    /** A grid whose labels {@code oracle} supplies, nothing computed yet. */
    public static Grid of(int rows, int cols, Vocabulary vocabulary, Oracle oracle) {
        if (rows < 0 || cols < 0) throw new IllegalArgumentException("grid " + rows + "x" + cols);
        return new Grid(rows, cols, vocabulary, new Memory(oracle, rows * cols), Map.of(), Map.of());
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public Vocabulary vocabulary() { return vocabulary; }

    /** Whether {@code (row, col)} is a cell of this grid. */
    public boolean has(int row, int col) { return row >= 0 && col >= 0 && row < rows && col < cols; }

    /** How many cells the oracle has been asked for, across every grid derived from this capture. */
    public int recognized() { return memory.asked; }

    /** The cell's current label: a state if a stage wrote one, else what was {@linkplain #read read}. */
    public String label(int row, int col) {
        if (!has(row, col)) throw new IndexOutOfBoundsException("(" + row + "," + col + ") of " + rows + "x" + cols);
        final var w = written.get(key(row, col));
        return w != null ? w : read(row, col);
    }

    /** What the oracle observed at the cell, whatever has been written over it since. Asked once. */
    public String read(int row, int col) {
        if (!has(row, col)) throw new IndexOutOfBoundsException("(" + row + "," + col + ") of " + rows + "x" + cols);
        final var i = row * cols + col;
        if (memory.labels[i] == null) {
            final var l = memory.oracle.label(row, col);
            if (l == null) throw new IllegalStateException("oracle gave no label for (" + row + "," + col + ")");
            if (vocabulary.states().contains(l))
                throw new IllegalStateException("oracle answered (" + row + "," + col + ") with '" + l
                        + "', a state: a state is written, never read");
            memory.labels[i] = l;
            memory.asked++;
        }
        return memory.labels[i];
    }

    /** Whether a stage wrote a state at the cell. */
    public boolean written(int row, int col) {
        return written.containsKey(key(row, col));
    }

    /**
     * Whether {@code (row, col)} is in {@code plane}. Off the grid only the vocabulary's outside planes
     * hold. On it, an everywhere plane or a marked plane answers without asking for the label.
     */
    public boolean in(String plane, int row, int col) {
        if (!has(row, col)) return vocabulary.outside().contains(plane);
        if (vocabulary.everywhere().contains(plane)) return true;
        if (marked(plane, row, col)) return true;
        return vocabulary.labels(plane, label(row, col));
    }

    /** The integer the cell's label carries, if the vocabulary gives it one. */
    public OptionalInt value(int row, int col) {
        if (!has(row, col)) return OptionalInt.empty();
        final var v = vocabulary.values().get(label(row, col));
        return v == null ? OptionalInt.empty() : OptionalInt.of(v);
    }

    /** This grid with the cell relabelled to a state. @throws IllegalArgumentException if it is not one */
    public Grid relabel(int row, int col, String label) {
        if (!has(row, col)) throw new IndexOutOfBoundsException("(" + row + "," + col + ")");
        if (!vocabulary.states().contains(label))
            throw new IllegalArgumentException("'" + label + "' is not a state, so no stage may write it");
        final var w = new HashMap<>(written);
        w.put(key(row, col), label);
        return new Grid(rows, cols, vocabulary, memory, Map.copyOf(w), marks);
    }

    /** This grid with the cell put in {@code plane}, carrying {@code note}. A cell keeps its first note. */
    public Grid mark(String plane, int row, int col, String note) {
        if (!has(row, col)) throw new IndexOutOfBoundsException("(" + row + "," + col + ")");
        final var existing = marks.getOrDefault(plane, Map.of());
        if (existing.containsKey(key(row, col))) return this;
        final var p = new HashMap<>(existing);
        p.put(key(row, col), note == null ? "" : note);
        final var m = new HashMap<>(marks);
        m.put(plane, Map.copyOf(p));
        return new Grid(rows, cols, vocabulary, memory, written, Map.copyOf(m));
    }

    /** Whether a stage marked the cell in {@code plane}. Never asks for the label. */
    public boolean marked(String plane, int row, int col) {
        final var p = marks.get(plane);
        return p != null && p.containsKey(key(row, col));
    }

    /** The note a stage left on the cell in {@code plane}. */
    public Optional<String> note(String plane, int row, int col) {
        final var p = marks.get(plane);
        return p == null ? Optional.empty() : Optional.ofNullable(p.get(key(row, col)));
    }

    /** Whether any cell is marked in {@code plane}. Never asks for a label. */
    public boolean anyMarked(String plane) {
        final var p = marks.get(plane);
        return p != null && !p.isEmpty();
    }

    /** The cells a stage marked in {@code plane}, in reading order, as {@code {row, col}}. Never asks for a label. */
    public List<int[]> markedCells(String plane) {
        final var p = marks.get(plane);
        if (p == null) return List.of();
        return p.keySet().stream().sorted()
                .map(k -> new int[]{(int) (k >> 32), (int) (long) k}).toList();
    }

    /** The cells of {@code plane}, in reading order, as {@code {row, col}}. Asks for every label it needs. */
    public List<int[]> cells(String plane) {
        final var out = new ArrayList<int[]>();
        for (var r = 0; r < rows; r++)
            for (var c = 0; c < cols; c++) if (in(plane, r, c)) out.add(new int[]{r, c});
        return out;
    }

    /** How many cells are in {@code plane}. */
    public int count(String plane) { return cells(plane).size(); }

    /** {@code plane} as a row-major binary grid, for a {@link Descriptor}. */
    public int[] plane(String plane) {
        final var out = new int[rows * cols];
        for (var r = 0; r < rows; r++)
            for (var c = 0; c < cols; c++) if (in(plane, r, c)) out[r * cols + c] = 1;
        return out;
    }

    private static long key(int row, int col) { return (((long) row) << 32) ^ (col & 0xffffffffL); }
}
