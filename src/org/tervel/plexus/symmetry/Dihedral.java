package org.tervel.plexus.symmetry;

import java.util.List;

/**
 * The dihedral symmetry group of a square (D4) and its subgroups. {@link #d4()} is the canonical
 * 8-transform group — identity, the three rotations, the two axis mirrors, the two diagonal mirrors
 * — in the order every descriptor bitmask in the system depends on. The subgroup produced by
 * {@link #rotations()} is a filter of that list, so its bit positions stay aligned with D4.
 */
public final class Dihedral implements SymmetryGroup {

    /** The 8 rigid transforms of a square, in canonical (bit-index) order. */
    private enum Move implements Transform {
        IDENTITY("identity", true)  { public int src(int r, int c, int s) { return r * s + c; } },
        ROT90   ("rot90",    true)  { public int src(int r, int c, int s) { return (s - 1 - c) * s + r; } },
        ROT180  ("rot180",   true)  { public int src(int r, int c, int s) { return (s - 1 - r) * s + (s - 1 - c); } },
        ROT270  ("rot270",   true)  { public int src(int r, int c, int s) { return c * s + (s - 1 - r); } },
        MIRROR_H("mirrorH",  false) { public int src(int r, int c, int s) { return (s - 1 - r) * s + c; } },
        MIRROR_V("mirrorV",  false) { public int src(int r, int c, int s) { return r * s + (s - 1 - c); } },
        DIAG_MAIN("diagMain", false){ public int src(int r, int c, int s) { return c * s + r; } },
        DIAG_ANTI("diagAnti", false){ public int src(int r, int c, int s) { return (s - 1 - c) * s + (s - 1 - r); } };

        private final String label;
        private final boolean rotation;
        Move(String label, boolean rotation) { this.label = label; this.rotation = rotation; }
        @Override public String label() { return label; }
        @Override public boolean isRotation() { return rotation; }
    }

    private final List<Transform> transforms;

    private Dihedral(List<Transform> transforms) { this.transforms = transforms; }

    /** The full dihedral group of the square, in canonical order. */
    public static SymmetryGroup d4() { return new Dihedral(List.of(Move.values())); }

    @Override public List<Transform> transforms() { return transforms; }

    @Override
    public SymmetryGroup rotations() {
        return new Dihedral(transforms.stream().filter(Transform::isRotation).toList());
    }
}
