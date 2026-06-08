package org.tervel.plexus.symmetry;

import java.util.Set;

/**
 * The <b>covering / non-covering split</b> for rotational symmetry orders — which n-fold symmetries can be a
 * {@link SymmetryGroup} over a periodic grid (a "cover"), and which cannot. This class is the seam between
 * the supported (crystallographic) track and the unbuilt (aperiodic) one.
 *
 * <h2>Covering shapes (the supported track)</h2>
 * A regular n-gon tiles the plane — covers it fully, edge to edge, with no gap — iff its rotational order is
 * <b>crystallographic</b>: {@code n ∈ {1, 2, 3, 4, 6}}. Three equivalent statements of the same fact:
 * <ul>
 *   <li>Euler's totient {@code φ(n) ≤ 2} ({@link #covers});</li>
 *   <li>the rotation's trace {@code 2·cos(2π/n)} is an integer, so it can act as an integer matrix in the
 *       lattice basis and map lattice points to lattice points;</li>
 *   <li>a whole number of interior angles sums to exactly 360° (triangle 6×60°, square 4×90°, hexagon
 *       3×120°). The pentagon's 108° misses — 3×108° = 324°, a 36° gap.</li>
 * </ul>
 * These are the only covers the Plexus router can route over. {@link Dihedral#d4()} (order 4, square) and
 * {@link Dihedral#d2()} (order 2, rectangle — the transpose-free subgroup) are the realised ones; the
 * rectangle is the first non-square domain, and threading its {@code (rows, cols)} frame through the routing
 * core ({@link org.tervel.plexus.invariants.Connectivity}, {@link org.tervel.plexus.invariants.Invariant#box},
 * and the descriptor geometry) is already done. The triangle (D3) and hexagon (D6) are covering-but-unbuilt,
 * and they share a single triangular/hexagonal lattice (the two are duals), so both follow from one new
 * lattice. The cost there is <em>not</em> the group — the {@link SymmetryGroup} seam is injected — it is the
 * geometry still hardcoded to a square in the heavier ops: {@code CellGraph}, the bounding-box enumeration
 * ({@code Shape}, {@code Fiber}), the k×k decimation ({@code Scale}). Lifting those onto a {@code Lattice}
 * abstraction is the remaining (bounded) work; the covering groups then plug in.
 *
 * <h2>Non-covering shapes (NOT supported)</h2>
 * A regular n-gon with {@code φ(n) > 2} ({@code n = 5, 7, 8, 9, 10, 12, …}) <b>cannot</b> tile the plane
 * periodically. The 5-fold symmetry is real but has nowhere flat-and-periodic to sit, so its surplus must go
 * somewhere — and each escape leaves this architecture entirely:
 * <ul>
 *   <li><b>curvature</b> — let the 36° deficit bend the surface; twelve pentagons close into a sphere, the
 *       dodecahedron (a Platonic solid). Total deficit 20×36° = 720° = 4π, by Gauss–Bonnet. A curved,
 *       non-Euclidean domain — not a planar grid.</li>
 *   <li><b>aperiodicity</b> — keep the plane flat but use two prototiles (kite/dart) that cover it with no
 *       period: a <b>Penrose</b> tiling, carrying the 5-fold symmetry a lone pentagon cannot.</li>
 *   <li><b>higher dimension</b> — {@code φ(n)} is the minimal lattice rank carrying n-fold symmetry
 *       ({@link #liftDimension}), so a pentagon ({@code φ(5) = 4}) is periodic only in 4-D; the planar
 *       Penrose tiling is the cut-and-project shadow of that 4-D lattice.</li>
 * </ul>
 *
 * <h2>TODO — the aperiodic track is not, and cannot directly be, a {@link SymmetryGroup}</h2>
 * Penrose / quasicrystalline covers break two load-bearing assumptions of this package, so they are left
 * unbuilt rather than faked:
 * <ol>
 *   <li><b>No finite symmetry group.</b> {@link SymmetryGroup} is a finite, ordered, index-as-bit-position
 *       list of {@link Transform}s with a bitmask {@link SymmetryGroup#stabilizer}. A Penrose cover has no
 *       such group: its symmetry is <em>inflation</em> (an infinite-order scaling by the golden ratio) plus a
 *       non-crystallographic point symmetry — a monoid, not a finite group. {@code canonicalStabilizer}
 *       (a minimum over a finite rotation subgroup) has nothing to fold over.</li>
 *   <li><b>No translation to quotient.</b> {@code Shape} and {@code Fiber} normalise by cropping to a
 *       bounding box — they factor out translation. An aperiodic cover has no translational symmetry, so
 *       that normalisation is meaningless.</li>
 * </ol>
 * The natural home for the aperiodic track is therefore <b>not here</b> but the {@code Scale} /
 * renormalisation seam — the inflation symmetry <em>is</em> a scale fold — plus a new cut-and-project
 * operator (lift to {@code φ(n)}-D and find the hidden periodicity; this is also the "forgery buster" for a
 * signal that looks aperiodic in 2-D yet is perfectly periodic upstairs). Until that is built,
 * <b>Penrose covers cannot be relied upon under this architecture.</b>
 *
 * <p>Discussion of record (the reasoning behind this split): the crystallographic restriction is governed by
 * the totient, not by primality — 2 and 3 are prime and cover; 8, 9, 10 are composite and do not. The clean
 * invariant is {@code φ(n) ≤ 2}, and {@code φ(n)} itself measures the dimension the symmetry is hiding in.
 */
public final class Crystallographic {

    private Crystallographic() { }

    /** The rotational orders whose regular polygon tiles the plane — the crystallographic restriction. */
    public static final Set<Integer> COVERING_ORDERS = Set.of(1, 2, 3, 4, 6);

    /**
     * Whether an n-fold rotation can be a covering {@link SymmetryGroup} over a periodic grid: true iff
     * {@code φ(n) ≤ 2}, i.e. {@code n ∈ {1, 2, 3, 4, 6}}. False (aperiodic, out of scope — see the class
     * TODO) otherwise.
     */
    public static boolean covers(int n) {
        return n >= 1 && totient(n) <= 2;
    }

    /**
     * The minimal lattice dimension that carries an n-fold rotation — Euler's totient {@code φ(n)}. For a
     * covering order this is ≤ 2 (it fits the plane); for a non-covering order it is the cut-and-project
     * dimension you must lift to (pentagon, octagon, decagon, dodecagon all {@code φ = 4}).
     */
    public static int liftDimension(int n) {
        return totient(n);
    }

    // TODO(aperiodic): no SymmetryGroup is provided for non-covering orders (covers(n) == false). Realising
    // them is the Scale/renormalisation + cut-and-project route described in this class's javadoc, not a new
    // Dihedral. Penrose covers are unsupported until that exists.

    /** Euler's totient {@code φ(n)}: the count of integers in {@code [1, n]} coprime to n ({@code φ(1) = 1}). */
    public static int totient(int n) {
        if (n < 1) throw new IllegalArgumentException("n must be >= 1: " + n);
        var result = n;
        var m = n;
        for (var p = 2; (long) p * p <= m; p++) {
            if (m % p == 0) {
                while (m % p == 0) m /= p;
                result -= result / p;
            }
        }
        if (m > 1) result -= result / m;        // m is the last prime factor
        return result;
    }
}
