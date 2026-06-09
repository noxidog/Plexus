package org.tervel.plexus.symmetry;

import java.util.List;

/**
 * The dihedral symmetry group of a square (D4) and its subgroups. {@link #d4()} is the canonical
 * 8-transform group — identity, the three rotations, the two axis mirrors, the two diagonal mirrors
 * — in the order every descriptor bitmask in the system depends on. The subgroup produced by
 * {@link #rotations()} is a filter of that list, so its bit positions stay aligned with D4.
 */
public final class Dihedral implements SymmetryGroup {

    /**
     * The 8 rigid transforms of a square, in canonical (bit-index) order. Each primitive is
     * {@code (rows, cols)}-aware; on a square frame ({@code rows == cols}) it reduces to the historical
     * single-side formula. The four <b>transposing</b> moves — {@code ROT90}, {@code ROT270},
     * {@code DIAG_MAIN}, {@code DIAG_ANTI} — swap the axes and are valid only when {@code rows == cols}; the
     * rectangular group {@link #d2()} excludes them.
     */
    private enum Move implements Transform {
        IDENTITY ("identity", true)  { public int src(int r, int c, int rows, int cols) { return r * cols + c; } },
        ROT90    ("rot90",    true)  { public int src(int r, int c, int rows, int cols) { return (rows - 1 - c) * cols + r; } },           // square-only (transposes)
        ROT180   ("rot180",   true)  { public int src(int r, int c, int rows, int cols) { return (rows - 1 - r) * cols + (cols - 1 - c); } },
        ROT270   ("rot270",   true)  { public int src(int r, int c, int rows, int cols) { return c * cols + (rows - 1 - r); } },           // square-only (transposes)
        MIRROR_H ("mirrorH",  false) { public int src(int r, int c, int rows, int cols) { return (rows - 1 - r) * cols + c; } },
        MIRROR_V ("mirrorV",  false) { public int src(int r, int c, int rows, int cols) { return r * cols + (cols - 1 - c); } },
        DIAG_MAIN("diagMain", false) { public int src(int r, int c, int rows, int cols) { return c * cols + r; } },                        // square-only (transposes)
        DIAG_ANTI("diagAnti", false) { public int src(int r, int c, int rows, int cols) { return (rows - 1 - c) * cols + (rows - 1 - r); } }; // square-only (transposes)

        private final String label;
        private final boolean rotation;
        Move(String label, boolean rotation) { this.label = label; this.rotation = rotation; }
        @Override public String label() { return label; }
        @Override public boolean isRotation() { return rotation; }
    }

    private final List<Transform> transforms;

    private Dihedral(List<Transform> transforms) { this.transforms = transforms; }

    /** The full dihedral group of the square, in canonical order (D4 — the covering order-4 domain). */
    public static SymmetryGroup d4() { return new Dihedral(List.of(Move.values())); }

    /**
     * The dihedral group of a rectangle (D2 — {@link Crystallographic#covers(int) covering} order 2): the
     * <b>transpose-free</b> subgroup {@code {identity, rot180, mirrorH, mirrorV}}. Breaking the square's
     * transpose symmetry (§11, "the rectangle") is exactly dropping {@code rot90/rot270} and the diagonal
     * mirrors, which would swap a non-square domain's two distinct axes. Use this for a rectangular domain.
     *
     * <p><b>Decidability lives in the break.</b> The broken transpose is what <em>lets</em> a structure
     * collapse to an answer: the asymmetry between the two axes is the routing coordinate. <b>Re-symmetrizing</b>
     * — restoring the transpose with {@link #d4()} — makes the two axes transpose-equivalent, i.e. a <em>twin</em>,
     * and so undecidable. The magnitude of the break is the confidence of the collapse (PLEXUS_DESIGN §11).
     */
    public static SymmetryGroup d2() {
        return new Dihedral(List.of(Move.IDENTITY, Move.ROT180, Move.MIRROR_H, Move.MIRROR_V));
    }

    @Override public List<Transform> transforms() { return transforms; }

    @Override
    public SymmetryGroup rotations() {
        return new Dihedral(transforms.stream().filter(Transform::isRotation).toList());
    }
}
