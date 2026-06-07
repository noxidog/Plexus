package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.ops.ThreadVolume;
import org.tervel.plexus.ops.Topology;

/**
 * The whole-table reading of <b>thread-volume = determinant</b>: walks the key trie ({@link Topology}) and,
 * for every context, prints the descriptor-fiber's volume measured five ways — {@code |fiber|} (full-frame
 * combinatorial), {@code |orbit|} (the rigid bbox-frame orbit), {@code [G:Stab]} (orbit-stabilizer index, the
 * group determinant), {@code √det(J Jᵀ)} (the Jacobian/coarea determinant), and {@code det L} (Matrix-Tree
 * spanning-tree count). The {@code id} column checks the exact identity {@code |orbit| == [G:Stab]} (the
 * orbit-stabilizer theorem) on the oriented track; {@code |fiber|} exceeds {@code |orbit|} by the translational
 * multiplicity. See {@link ThreadVolume} for the maths.
 *
 * <p>This sits in the generate/whole-table menu cell next to {@code possibility}: it is the same fiber
 * enumeration, but <em>measured</em> rather than listed.
 */
public final class VolumeReport implements Report {

    private final int side;

    public VolumeReport(int side) { this.side = side; }

    @Override
    public String apply(Plexus plexus) {
        final var sb = new StringBuilder();
        sb.append("VOLUME = DETERMINANT  (the thread D^-1(key) measured five ways; |orbit| = [G:Stab] is the identity)\n\n");
        sb.append(String.format("  %-24s %8s %7s %9s %12s %11s  %s%n",
                "key", "|fiber|", "|orbit|", "[G:Stab]", "sqrt detJJ'", "#span-tree", "id"));
        sb.append("  ").append("-".repeat(84)).append('\n');

        final var holds = new long[1];
        final var oriented = new long[1];
        new Topology(node -> {
            final var m = new ThreadVolume(node.key(), side).apply(plexus);
            var label = (node.radial() ? "radial" : plexus.group().describe(node.key().symmetry()))
                    + " " + node.coords();
            if (label.length() > 24) label = label.substring(0, 24);
            if (!m.radial()) { oriented[0]++; if (m.identityHolds()) holds[0]++; }
            sb.append(String.format("  %-24s %8d %7d %9s %12.2f %11d  %s%n",
                    label, m.fiber(), m.orbit(),
                    m.radial() ? "n/a" : Long.toString(m.index()),
                    m.gram(), m.spanningTrees(),
                    m.radial() ? "n/a*" : (m.identityHolds() ? "ok" : "MISMATCH")));
        }).apply(plexus);

        sb.append("  ").append("-".repeat(84)).append('\n');
        sb.append(String.format("  identity |orbit| = [G:Stab] (orbit-stabilizer) holds: %d/%d oriented contexts%n%n",
                holds[0], oriented[0]));
        sb.append("  legend\n");
        sb.append("    |fiber|       = #grids routing here (Fiber.count) - the full-frame combinatorial volume, entropy = log of it\n");
        sb.append("    |orbit|       = distinct rigid images of the centred shape - the bbox-frame thread (translation quotiented out)\n");
        sb.append("    [G:Stab]      = |G| / |Stab| - the orbit-stabilizer index, the group determinant of the action\n");
        sb.append("    sqrt detJJ'   = Gram determinant of the invariant-component gradients - the Jacobian/coarea volume\n");
        sb.append("    #span-tree    = det of the reduced cell-graph Laplacian (Matrix-Tree) - the graph-theoretic volume\n");
        sb.append("    id            = does |orbit| = [G:Stab]?  |fiber|/|orbit| = translational multiplicity;  n/a* = radial track\n");
        return sb.toString();
    }
}
