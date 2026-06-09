package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.ops.PrimeFrame;

/**
 * The frame-arithmetic reading: <b>is the {@code n}-cell observation window prime — equivalently, scale-rigid?</b>
 * Renders {@link PrimeFrame}: the divisor lattice of the frame, the orbit-size histogram under the cyclic
 * group {@code Z_n} (the translational action the router quotients away, §6), the exact primality verdict
 * (every non-constant orbit has size {@code n}), the Fermat corollary {@code 2^n ≡ 2 (mod n)} flagged as a
 * necessary-only proxy, and the §11 reading that a prime frame is one {@link org.tervel.plexus.ops.Scale}
 * cannot coarse-grain.
 *
 * <p>It is a <b>frame-level</b> diagnostic — a property of {@code n} itself, not of any context or trie node —
 * so it sits beside {@code [s] scale} (its change-of-scale sibling) rather than on the classify/generate ×
 * node/table grid the other reports ride. The distribution of primes is <em>not</em> reported: that spacing
 * law is the analytic residual the symmetry points at but does not derive (§5, §11 caveat).
 */
public final class PrimeReport implements Report {

    private final int side;

    public PrimeReport(int side) { this.side = side; }

    @Override
    public String apply(Plexus plexus) {
        final var r = new PrimeFrame(side).apply(plexus);
        final var sb = new StringBuilder();
        sb.append("FRAME ARITHMETIC  -  is the n-cell observation window prime / scale-rigid?\n\n");
        sb.append(String.format("  side n        = %d%n", r.side()));
        sb.append(String.format("  divisors of n = %s%n", r.divisors()));
        sb.append(String.format("  prime?        = %s   (the realizable orbit sizes are exactly {1, n})%n",
                yesno(r.prime())));
        sb.append(String.format("  scale-rigid?  = %s   (no k | n with 1 < k < n  =>  Scale (sec.11) cannot coarse-grain)%n%n",
                yesno(r.scaleRigid())));

        sb.append("  orbit sizes under cyclic Z_n  (size = minimal period; # = number of orbits)\n");
        if (r.orbitSizes().isEmpty()) {
            sb.append("    (counts omitted: n too large to enumerate)\n");
        } else {
            r.orbitSizes().forEach((size, count) -> sb.append(String.format(
                    "    size %4d : %8d orbits%s%n", size, count,
                    size == 1 ? "    (the constants - all-0, all-1)" : "")));
            sb.append(String.format("    => every non-constant orbit has size n: %s  =>  n is %s   [exact]%n",
                    yesno(r.prime()), r.prime() ? "prime" : "not prime"));
            sb.append(String.format("    verified by brute enumeration: %s%n", yesno(r.verifiedByEnumeration())));
        }

        sb.append(String.format("%n  Fermat corollary = 2^n mod n = %d   (holds: %s)%n",
                r.fermatResidue(), yesno(r.fermatHolds())));
        sb.append("    note: 2^n = 2 (mod n) is necessary, not sufficient - pseudoprimes pass it;\n");
        sb.append("          the orbit-size test above is the exact discriminator.\n\n");

        sb.append("  caveat: this measures ONE frame. The DISTRIBUTION of primes (how the scale-rigid n thin\n");
        sb.append("          out) is the analytic residual the symmetry points at but does not derive (sec.5, sec.11).\n");
        return sb.toString();
    }

    private static String yesno(boolean b) { return b ? "yes" : "no"; }
}
