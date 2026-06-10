package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.ops.Topology;

import java.util.function.Predicate;

/**
 * A slice of the context table: walk the {@link Topology} automaton and list the nodes matching a partial
 * key (a {@link Predicate} over the node). Querying by a discriminator ({@code sig=167}) or by label
 * ({@code TARGET}) are both just predicates over a node's named coordinates or its answer — the same walk,
 * a different filter. This is the generic "fix some columns, list the matching contexts" operation.
 */
public class SliceReport implements Report {

    private final String header;
    private final Predicate<Topology.Node> match;

    public SliceReport(String header, Predicate<Topology.Node> match) {
        this.header = header;
        this.match = match;
    }

    @Override
    public String apply(Plexus plexus) {
        final var sb = new StringBuilder(header).append(":\n");
        final var count = new int[1];
        new Topology(node -> {
            if (!match.test(node)) return;
            count[0]++;
            final var c = node.context();
            final var sym = node.radial() ? "(radial)" : "[" + node.symmetryLabel() + "]";
            sb.append(String.format("  |- %-34s %-22s %s  pos=%d neg=%d exc=%d%n",
                    sym, node.coords(), c.isTarget() ? "TARGET" : "reject", c.pos(), c.neg(), c.exceptionCount()));
        }).apply(plexus);
        if (count[0] == 0) sb.append("  (none)\n");
        else sb.append("  ").append(count[0]).append(" contexts\n");
        return sb.toString();
    }
}
