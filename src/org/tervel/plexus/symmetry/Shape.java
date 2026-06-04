package org.tervel.plexus.symmetry;

import java.util.Arrays;

/**
 * A shape as itself. The active cells are cropped to their <b>smallest bounding rectangle</b> (translation
 * quotiented away), folded to the group-<b>canonical pose</b> (orientation quotiented away), and tagged
 * with their <b>isotropy fraction</b> {@code |Stab| / |G|} — the relative size of the fundamental domain
 * the shape folds into. That fraction is the dimension-free generalization of "which octant": a shape with
 * no symmetry folds into a single fundamental domain ({@code 1/|G|}, e.g. {@code 1/8} of a square under
 * D4), and the more symmetry it has the larger the domain ({@code 1/4} for a single mirror, {@code 1} for
 * full symmetry). By orbit–stabilizer it is exactly {@code 1 / |orbit|}.
 *
 * <p>What survives is the shape's frame-free identity — {@code (cells, height, width)} — paired with how
 * much symmetry it carries — {@code isotropy}. Both are orbit-invariants, so the pair is a complete
 * symmetry-address: <i>what it is up to symmetry</i>, and <i>how symmetric it is</i>.
 */
public record Shape(int[] cells, int height, int width, double isotropy) {

    /** Crop {@code grid} to its bounding box, fold to the group-canonical pose, and measure isotropy. */
    public static Shape of(int[] grid, SymmetryGroup group) {
        final var transforms = group.transforms();
        final var self = crop(grid);
        var canonical = self;
        var fixers = 0;
        for (final var t : transforms) {
            final var image = crop(t.apply(grid));
            if (image.equals(self)) fixers++;
            if (image.compareTo(canonical) < 0) canonical = image;
        }
        return new Shape(canonical.cells, canonical.h, canonical.w, (double) fixers / transforms.size());
    }

    /** Embed the canonical pattern at the top-left of a fresh {@code side×side} grid. */
    public int[] embed(int side) {
        final var g = new int[side * side];
        for (var r = 0; r < height; r++) for (var c = 0; c < width; c++)
            if (cells[r * width + c] == 1) g[r * side + c] = 1;
        return g;
    }

    /** The isotropy fraction rendered as {@code 1/|orbit|}, e.g. {@code "1/4"}. */
    public String isotropyFraction() {
        return isotropy <= 0 ? "0" : "1/" + Math.round(1.0 / isotropy);
    }

    /** Value equality on the canonical pattern; isotropy is derived from it, so it is not compared. */
    @Override public boolean equals(Object o) {
        return o instanceof Shape s && height == s.height && width == s.width && Arrays.equals(cells, s.cells);
    }
    @Override public int hashCode() { return Arrays.hashCode(cells) * 31 + height * 31 + width; }

    // --- a translation-normalized rectangular pattern, ordered for canonicalization ------------

    private record Crop(int[] cells, int h, int w) implements Comparable<Crop> {
        @Override public boolean equals(Object o) {
            return o instanceof Crop c && h == c.h && w == c.w && Arrays.equals(cells, c.cells);
        }
        @Override public int hashCode() { return Arrays.hashCode(cells) * 31 + h * 31 + w; }
        @Override public int compareTo(Crop o) {
            if (h != o.h) return Integer.compare(h, o.h);
            if (w != o.w) return Integer.compare(w, o.w);
            return Arrays.compare(cells, o.cells);
        }
    }

    private static Crop crop(int[] grid) {
        final var side = (int) Math.round(Math.sqrt(grid.length));
        var minR = side; var maxR = -1; var minC = side; var maxC = -1;
        for (var i = 0; i < grid.length; i++) if (grid[i] == 1) {
            final int r = i / side, c = i % side;
            minR = Math.min(minR, r); maxR = Math.max(maxR, r);
            minC = Math.min(minC, c); maxC = Math.max(maxC, c);
        }
        if (maxR < 0) return new Crop(new int[0], 0, 0);
        final var h = maxR - minR + 1;
        final var w = maxC - minC + 1;
        final var cells = new int[h * w];
        for (var r = 0; r < h; r++) for (var c = 0; c < w; c++)
            cells[r * w + c] = grid[(minR + r) * side + (minC + c)];
        return new Crop(cells, h, w);
    }
}
