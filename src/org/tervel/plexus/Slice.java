package org.tervel.plexus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A slice query: which root(s) to walk, a constraint per key level, the label to keep, and the extrapolation setting. Levels
 * left unconstrained match anything. Symmetry names are the walk's own names for the box kind (a rectangle's
 * {@code "short-axis mirror"}, a square box's {@code "mirrorH"}, a hex shape's {@code "mirror30"}); a required
 * name that a box's walk does not have rules that box out.
 *
 * <pre>{@code
 * router.slice(Slice.on(Root.D4).box(2, 3).atLeast("short-axis mirror").mass(4, 5).joins(3, 4).label(1),
 *              (grid, context, evidence) -> ...);
 * }</pre>
 */
public final class Slice {

    /** Whether a generated grid's own path was trained, or it was answered by a pure node above it. */
    public enum Evidence { CONFIRMED, ASSUMED }

    /** Receives each grid a slice produces, with the node that controls its label. */
    @FunctionalInterface
    public interface Callback { void accept(int[] grid, Context context, Evidence evidence); }

    /** What a slice produced: entries per kind of evidence, and nodes whose generation was skipped as too large. */
    public record Result(int confirmed, int assumed, int skipped) { }

    /** What one delivered entry stands for. */
    public enum Per {
        /** Every matching grid: each placement of each pose of each shape. The default. */
        PLACEMENT,
        /** One grid per distinct pose of each shape: the positions it was found at are folded together. */
        POSE,
        /** One grid per distinct shape: its poses and positions are folded together. */
        SHAPE
    }

    final Root root;
    Integer boxShort;
    Integer boxLong;
    final Set<String> required = new LinkedHashSet<>();
    boolean exact;
    int massMin = 0, massMax = Integer.MAX_VALUE;
    int joinsMin = 0, joinsMax = Integer.MAX_VALUE;
    Integer label;
    Integer orientation;
    Integer pose;
    Integer cornerRow;
    Integer cornerCol;
    Per per = Per.PLACEMENT;
    Extrapolate extrapolate = Extrapolate.SYMMETRY;

    private Slice(Root root) { this.root = root; }

    /** Start a slice over {@code root} ({@link Root#BOTH} walks the D4 root, then the D6 root). */
    public static Slice on(Root root) { return new Slice(root); }

    /** Only this box: short × long on D4 (order does not matter). */
    public Slice box(int a, int b) { boxShort = Math.min(a, b); boxLong = Math.max(a, b); return this; }

    /** Only this box: the common extent of a hex shape, or a square box of this side. */
    public Slice box(int extent) { return box(extent, extent); }

    /** The shape keeps at least these symmetries. Names are resolved by {@link #symmetryName}. */
    public Slice atLeast(String... names) {
        for (final var n : names) required.add(symmetryName(n));
        exact = false;
        return this;
    }

    /**
     * The shape has exactly these symmetries and no others. List a closed set (rot180 with both mirrors).
     * Names are resolved by {@link #symmetryName}.
     */
    public Slice exactly(String... names) {
        for (final var n : names) required.add(symmetryName(n));
        exact = true;
        return this;
    }

    /** Every symmetry a walk asks about, across the three kinds of box, without repeats. */
    public static List<String> symmetryNames() {
        final var out = new LinkedHashSet<String>();
        for (final var kind : List.of(Kind.RECT, Kind.SQUARE, Kind.HEX))
            for (final var m : Groups.walkFor(kind)) out.add(m.name());
        return List.copyOf(out);
    }

    /**
     * The walk's name for {@code typed}, matched without regard to case, spaces or hyphens, so
     * {@code "long-axis-mirror"} and {@code "Long Axis Mirror"} both mean {@code "long-axis mirror"}.
     *
     * @throws IllegalArgumentException if no walk asks about such a symmetry; the message lists the names
     */
    public static String symmetryName(String typed) {
        final var key = squash(typed);
        for (final var name : symmetryNames()) if (squash(name).equals(key)) return name;
        throw new IllegalArgumentException("unknown symmetry \"" + typed.trim() + "\"; the names are "
                + String.join(", ", symmetryNames()));
    }

    private static String squash(String s) { return s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", ""); }

    public Slice mass(int min, int max) { massMin = min; massMax = max; return this; }
    public Slice joins(int min, int max) { joinsMin = min; joinsMax = max; return this; }
    public Slice label(int label) { this.label = label; return this; }

    /**
     * Only this orientation: on a rectangle 0 is not turned and 1 is turned to reach landscape; on hex 0 is
     * rows shifted right and 1 left. A square box has no orientation, so this rules square boxes out.
     */
    public Slice orientation(int orientation) { this.orientation = orientation; return this; }

    /** Only this pose code: the transform that reached the canonical pose; 0 is the canonical pose itself. */
    public Slice pose(int pose) { this.pose = pose; return this; }

    /** Only grids whose box has its top-left corner at this row and column of the input frame. */
    public Slice corner(int row, int col) { cornerRow = row; cornerCol = col; return this; }

    /** Deliver one entry per placement (the default), per distinct pose, or per distinct shape. */
    public Slice per(Per per) { this.per = per; return this; }
    /** How far the slice may extrapolate; {@link Extrapolate#SYMMETRY} unless set. */
    public Slice extrapolate(Extrapolate extrapolate) { this.extrapolate = extrapolate; return this; }

    /** Whether a described grid satisfies every constraint but the label (used on generated candidates). */
    boolean admits(Descriptor d) {
        if (orientation != null && d.orientation() != orientation) return false;
        if (pose != null && d.pose() != pose) return false;
        if (cornerRow != null && (d.cornerRow() != cornerRow || d.cornerCol() != cornerCol)) return false;
        if (boxShort != null && (d.boxShort() != boxShort || d.boxLong() != boxLong)) return false;
        if (d.mass() < massMin || d.mass() > massMax || d.joins() < joinsMin || d.joins() > joinsMax) return false;
        final var names = d.walkNames();
        if (!names.containsAll(required) && !required.isEmpty()) return false;
        final var have = d.symmetries();
        if (!have.containsAll(required)) return false;
        return !exact || have.size() == required.size();
    }
}
