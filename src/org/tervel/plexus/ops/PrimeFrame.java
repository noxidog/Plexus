package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <b>The frame's own arithmetic: is the {@code n}-cell observation window prime?</b> This measures the part
 * of the frame the router deliberately throws away — the <em>translational</em> action. Plexus quotients
 * translation out by cropping to the bounding box (§6); the cyclic group {@code Z_n} that translation forms
 * is therefore <em>not</em> part of the injected D4 {@link org.tervel.plexus.symmetry.SymmetryGroup}, and this
 * op carries its own cyclic shift rather than borrowing the group. That is the honest seam: this reads the
 * structure the descriptor discards.
 *
 * <h2>Primality is scale-rigidity</h2>
 * The subgroup lattice of {@code Z_n} <em>is</em> the divisor lattice of {@code n} — one subgroup per divisor.
 * So:
 * <pre>
 *   n is prime  ⟺  Z_n has no proper non-trivial subgroup  ⟺  the frame has no sub-period
 *               ⟺  no k | n with 1 &lt; k &lt; n  ⟺  {@link Scale} cannot coarse-grain (§11, change of scale)
 * </pre>
 * A composite frame can be tiled by a smaller repeating block (a pattern of period {@code d | n} is fixed by
 * translation-by-{@code d}); a prime frame cannot. In §11's terms a prime is a <b>scale-rigid</b> observation
 * window — the renormalization knob is blocked, only {@code k ∈ {1, n}} divides it.
 *
 * <h2>The orbit law (Fermat falls out)</h2>
 * Act with {@code Z_n} on every length-{@code n} binary row. By orbit–stabilizer each orbit's size is the
 * string's <b>minimal period</b>, which divides {@code n}. So the multiset of orbit sizes is exactly the set
 * of divisors of {@code n}. The <b>exact</b> primality test is therefore:
 * <blockquote>every non-constant orbit has size {@code n} ⟺ the only divisors are {@code 1} and {@code n} ⟺
 * {@code n} is prime.</blockquote>
 * Counting the {@code 2^n} rows when {@code n} is prime: two are constant (orbit size 1), the rest split into
 * orbits of size exactly {@code n}, so {@code n | 2^n − 2}, i.e. {@code 2^n ≡ 2 (mod n)} — Fermat's little
 * theorem, as a consequence of the frame being indecomposable. That congruence is exposed as a
 * <em>corollary</em>, but flagged a <b>proxy</b>: {@code 2^n ≡ 2 (mod n)} is necessary, not sufficient
 * (pseudoprimes pass it), exactly as the bounding box is a proxy the {@link org.tervel.plexus.invariants.Signature}
 * is the exact form of (§3). The orbit-size test above is the exact one.
 *
 * <h2>What this does NOT do</h2>
 * It measures <em>one</em> frame. The <b>distribution</b> of primes — how the scale-rigid {@code n} thin out
 * ({@code ~n/ln n}, the gaps) — is the analytic residual the symmetry points at but does not derive (§5, and
 * the §11 caveat). This op characterizes primality structurally and checks the Fermat corollary; it makes no
 * claim about density.
 */
public final class PrimeFrame implements Operation<PrimeFrame.Reading> {

    /** Largest side enumerated brute-force (2^n rows) to <em>verify</em> the analytic orbit counts. */
    private static final int MAX_ENUM = 22;
    /** Largest side whose analytic orbit counts stay within {@code long} (uses {@code 2^e}, e ≤ n). */
    private static final int MAX_COUNT = 62;

    /**
     * A frame's translational reading.
     *
     * @param side                 the linear frame dimension {@code n}
     * @param divisors             the divisors of {@code n} (= the realizable orbit sizes)
     * @param orbitSizes           orbit size → number of orbits of that size (empty if {@code n} too large to count)
     * @param constants            number of size-1 orbits (the constant rows: all-0, all-1)
     * @param nonConstantOrbits    number of orbits of size &gt; 1
     * @param prime                exact: every non-constant orbit has size {@code n}
     * @param scaleRigid           no {@code k | n} with {@code 1 < k < n} (equivalent to {@code prime}; the §11 reading)
     * @param fermatResidue        {@code 2^n mod n}
     * @param fermatHolds          {@code 2^n ≡ 2 (mod n)} — the necessary-only Fermat corollary
     * @param verifiedByEnumeration whether the analytic counts were re-derived by brute enumeration
     */
    public record Reading(int side, List<Integer> divisors, SortedMap<Integer, Long> orbitSizes,
                          long constants, long nonConstantOrbits, boolean prime, boolean scaleRigid,
                          long fermatResidue, boolean fermatHolds, boolean verifiedByEnumeration) { }

    private final int side;

    public PrimeFrame(int side) { this.side = side; }

    @Override
    public Reading apply(Plexus plexus) {
        final var divisors = divisors(side);
        // Orbit size d occurs for every divisor d|n, with primitiveNecklaces(d) orbits of that size.
        final var sizes = new TreeMap<Integer, Long>();
        if (side >= 1 && side <= MAX_COUNT)
            for (final var d : divisors) sizes.put(d, primitiveNecklaces(d));

        final long constants = sizes.getOrDefault(1, 0L);
        final long nonConstant = sizes.entrySet().stream()
                .filter(e -> e.getKey() > 1).mapToLong(java.util.Map.Entry::getValue).sum();

        // EXACT primality test: the realizable orbit sizes (= divisors) are exactly {1, n}.
        final boolean prime = side > 1 && divisors.size() == 2;
        final long residue = modPow(2, side, side);
        final boolean fermat = side >= 1 && residue == Math.floorMod(2, side);

        final boolean verified = side <= MAX_ENUM && !sizes.isEmpty() && enumerationAgrees(sizes);
        return new Reading(side, divisors, sizes, constants, nonConstant, prime, prime,
                residue, fermat, verified);
    }

    // ---- brute verification: watch the rigidity happen ------------------------------------------

    /** Re-derive the orbit-size histogram by shifting every length-{@code n} binary row; must match {@code sizes}. */
    private boolean enumerationAgrees(SortedMap<Integer, Long> sizes) {
        final long mask = (1L << side) - 1;
        final var seen = new java.util.HashSet<Long>();
        final var measured = new TreeMap<Integer, Long>();
        for (long w = 0; w <= mask; w++) {
            if (!seen.add(canonical(w, mask))) continue;   // one representative per orbit
            measured.merge(minimalPeriod(w, mask), 1L, Long::sum);
        }
        return measured.equals(sizes);
    }

    /** Orbit size = minimal rotation period = smallest {@code d | n} with {@code rotate(w, d) == w}. */
    private int minimalPeriod(long w, long mask) {
        for (final var d : divisors(side)) if (rotate(w, d, mask) == w) return d;
        return side;   // unreachable: n itself always rotates to identity
    }

    /** The orbit's identifier: the least value among all {@code n} rotations of {@code w}. */
    private long canonical(long w, long mask) {
        var best = w;
        for (var k = 1; k < side; k++) best = Math.min(best, rotate(w, k, mask));
        return best;
    }

    /** Cyclic left-rotate the low {@code n} bits of {@code w} by {@code k} (0 ≤ k ≤ n). */
    private long rotate(long w, int k, long mask) {
        if (k == 0 || k == side) return w;
        return ((w << k) | (w >>> (side - k))) & mask;
    }

    // ---- closed-form counts ---------------------------------------------------------------------

    /** Number of primitive (aperiodic) binary necklaces of length {@code d} — the count of size-{@code d} orbits. */
    private static long primitiveNecklaces(int d) {
        long sum = 0;
        for (var e = 1; e <= d; e++) if (d % e == 0) sum += (long) mobius(d / e) * (1L << e);
        return sum / d;
    }

    /** Möbius function μ(m): 0 on square factors, else (−1)^(#distinct primes). */
    private static int mobius(int m) {
        if (m == 1) return 1;
        var primes = 0;
        for (var p = 2; (long) p * p <= m; p++) {
            if (m % p == 0) {
                m /= p;
                if (m % p == 0) return 0;   // p² | m
                primes++;
            }
        }
        if (m > 1) primes++;
        return (primes & 1) == 0 ? 1 : -1;
    }

    /** Divisors of {@code n} in ascending order ({@code {1}} for n ≤ 1). */
    private static List<Integer> divisors(int n) {
        final var out = new ArrayList<Integer>();
        if (n < 1) return List.of();
        for (var d = 1; d <= n; d++) if (n % d == 0) out.add(d);
        return out;
    }

    /** {@code base^exp mod m} by fast exponentiation ({@code m} small, so products stay in range). */
    private static long modPow(long base, long exp, long m) {
        if (m <= 1) return 0;
        long r = 1;
        base %= m;
        while (exp > 0) {
            if ((exp & 1) == 1) r = r * base % m;
            base = base * base % m;
            exp >>= 1;
        }
        return r;
    }
}
