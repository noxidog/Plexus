package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.Plexus.Context;
import org.tervel.plexus.ops.Topology;

/**
 * The printing consumer of the {@link Topology} automaton: walks the key trie and renders each oriented
 * node as a line — class imbalance (pos / neg and neg:pos ratio), exception count, and the broken
 * symmetries (if any). Circular (radial / dot) contexts are hidden. The traversal lives in {@link
 * Topology}; this class only decides what a node looks like as text.
 */
public class TopologyReport implements Report {

    @Override
    public String apply(Plexus plexus) {
        final var contexts = plexus.contexts();
        final var targets = contexts.values().stream().filter(Context::isTarget).count();

        final var sb = new StringBuilder();
        sb.append("Target contexts: ").append(targets)
          .append(" (of ").append(contexts.size()).append(")\n");
        sb.append("\nKey trie - oriented shapes (dot/circular contexts hidden)\n");

        new Topology(node -> print(sb, node)).apply(plexus);
        return sb.toString();
    }

    /** Render one oriented node; radial nodes are skipped. */
    private static void print(StringBuilder sb, Topology.Node node) {
        if (node.radial()) return;
        if (node.groupBoundary()) sb.append(String.format("%n[%s]%n", node.symmetryLabel()));

        final var c = node.context();
        final var ratio = c.pos() == 0 ? "all-neg" : String.format("%.2f:1", (double) c.neg() / c.pos());
        final var kind = c.isTarget() ? "TARGET" : "reject";
        final var broken = node.brokenSymmetries().isEmpty() ? ""
                : " BROKEN[" + String.join(", ", node.brokenSymmetries()) + "]";
        sb.append(String.format("  |- %-18s %s  pos=%-4d neg=%-5d  neg:pos=%-8s exc=%d%s%n",
                node.coords(), kind, c.pos(), c.neg(), ratio, c.exceptionCount(), broken));
    }
}
