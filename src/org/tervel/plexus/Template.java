package org.tervel.plexus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A small pattern of named cells around an <b>anchor</b>, matched on a {@link Grid} by <b>containment</b>:
 * it holds at a cell when every one of its cells is in the plane it names. Cells it does not name are not
 * looked at, which is what lets a template sit on a grid carrying more than it asks for.
 *
 * <p>A template is stored as offsets from its anchor, so it is cropped to its own cells by construction
 * and its size is whatever it was declared with. Its <b>poses</b> are its images under the square's eight
 * turns and mirrors about the anchor, the anchor staying put; images that coincide are kept once, so a
 * symmetric template has |D4| ÷ |Stab| of them. Each pose records the move that produced it from the
 * declared one. The smallest pose is the <b>canonical</b> one, and two templates are equal when their
 * canonical poses are: a template and its rotation are the same template.
 */
public final class Template {

    /** One cell of a pose: its offset from the anchor and the plane it must be in. */
    public record Cell(int row, int col, String plane) { }

    /** One image of the template, and the move that turned the declared template into it. */
    public record Pose(String move, List<Cell> cells) {

        /** The cells named {@code plane}. */
        public List<Cell> named(String plane) {
            return cells.stream().filter(c -> c.plane().equals(plane)).toList();
        }
    }

    /** A pose sitting on a grid, its anchor at {@code (row, col)}. */
    public record Placement(Pose pose, int row, int col) {

        /** The grid positions of the cells named {@code plane}, as {@code {row, col}}. */
        public List<int[]> at(String plane) {
            return pose.named(plane).stream().map(c -> new int[]{row + c.row(), col + c.col()}).toList();
        }

        /** How many of the cells named {@code role} are in {@code plane} on {@code grid}. */
        public int count(Grid grid, String role, String plane) {
            var n = 0;
            for (final var c : pose.named(role)) if (grid.in(plane, row + c.row(), col + c.col())) n++;
            return n;
        }
    }

    private static final Comparator<Cell> ORDER = Comparator.comparingInt(Cell::row)
            .thenComparingInt(Cell::col).thenComparing(Cell::plane);

    private final List<Pose> poses;   // distinct images; the declared one first

    private Template(List<Pose> poses) { this.poses = poses; }

    /**
     * A template from declared rows: {@code null} (or empty) is not part of it, any other entry is the
     * plane that cell must be in. {@code (anchorRow, anchorCol)} is the anchor and must be one of its cells.
     */
    public static Template of(String[][] rows, int anchorRow, int anchorCol) {
        if (anchorRow < 0 || anchorRow >= rows.length || anchorCol < 0 || anchorCol >= rows[anchorRow].length
                || blank(rows[anchorRow][anchorCol]))
            throw new IllegalArgumentException("the anchor (" + anchorRow + "," + anchorCol + ") is not a cell of the template");
        final var declared = new ArrayList<Cell>();
        for (var r = 0; r < rows.length; r++)
            for (var c = 0; c < rows[r].length; c++)
                if (!blank(rows[r][c])) declared.add(new Cell(r - anchorRow, c - anchorCol, rows[r][c]));
        final var seen = new LinkedHashSet<List<Cell>>();
        final var poses = new ArrayList<Pose>();
        for (final var g : Groups.D4) {
            final var image = declared.stream()
                    .map(x -> new Cell(g.fa().applyAsInt(x.row(), x.col()), g.fb().applyAsInt(x.row(), x.col()), x.plane()))
                    .sorted(ORDER).toList();
            if (seen.add(image)) poses.add(new Pose(g.name(), image));
        }
        return new Template(List.copyOf(poses));
    }

    private static boolean blank(String s) { return s == null || s.isEmpty(); }

    /** The distinct poses, the declared one first. */
    public List<Pose> poses() { return poses; }

    /** The smallest pose: the same for every declared orientation of this template. */
    public Pose canonical() {
        return poses.stream().min((x, y) -> compare(x.cells(), y.cells())).orElseThrow();
    }

    /** Rows × columns of the declared cells' box. */
    public int[] box() {
        final var cells = poses.get(0).cells();
        final var rMin = cells.stream().mapToInt(Cell::row).min().orElse(0);
        final var rMax = cells.stream().mapToInt(Cell::row).max().orElse(0);
        final var cMin = cells.stream().mapToInt(Cell::col).min().orElse(0);
        final var cMax = cells.stream().mapToInt(Cell::col).max().orElse(0);
        return new int[]{rMax - rMin + 1, cMax - cMin + 1};
    }

    /** The plane the anchor itself must be in. */
    public String anchorPlane() {
        return poses.get(0).cells().stream().filter(c -> c.row() == 0 && c.col() == 0).findFirst().orElseThrow().plane();
    }

    /** The first pose that holds with its anchor at {@code (row, col)}; every cell checked, the first miss rejects. */
    public Optional<Pose> poseAt(Grid grid, int row, int col) {
        for (final var p : poses) if (holds(p, grid, row, col)) return Optional.of(p);
        return Optional.empty();
    }

    /** Whether some pose holds with its anchor at {@code (row, col)}. */
    public boolean matches(Grid grid, int row, int col) { return poseAt(grid, row, col).isPresent(); }

    /**
     * Every placement that holds and has one of its cells named {@code role} on {@code (row, col)}: the
     * ways this template can sit so that the cell is part of it in that role.
     */
    public List<Placement> covering(Grid grid, int row, int col, String role) {
        final var out = new ArrayList<Placement>();
        final var seen = new LinkedHashSet<String>();
        for (final var p : poses)
            for (final var c : p.named(role)) {
                final int ar = row - c.row(), ac = col - c.col();
                if (!grid.has(ar, ac) && !grid.vocabulary().outside().contains(anchorPlane())) continue;
                if (!holds(p, grid, ar, ac)) continue;
                if (seen.add(ar + "," + ac + "," + p.cells())) out.add(new Placement(p, ar, ac));
            }
        return out;
    }

    private static boolean holds(Pose p, Grid grid, int row, int col) {
        for (final var c : p.cells()) if (!grid.in(c.plane(), row + c.row(), col + c.col())) return false;
        return true;
    }

    private static int compare(List<Cell> a, List<Cell> b) {
        for (var i = 0; i < Math.min(a.size(), b.size()); i++) {
            final var c = ORDER.compare(a.get(i), b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }

    @Override public boolean equals(Object o) { return o instanceof Template t && canonical().cells().equals(t.canonical().cells()); }
    @Override public int hashCode() { return Objects.hash(canonical().cells()); }
    @Override public String toString() { return "template" + canonical().cells(); }
}
