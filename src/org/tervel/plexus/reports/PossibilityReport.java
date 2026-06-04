package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;
import org.tervel.plexus.ops.Fiber;
import org.tervel.plexus.ops.Topology;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A possibility-space consumer of the {@link Topology} automaton. Where {@link TopologyReport} prints the
 * counts training <em>observed</em> at each symmetry-break, this generates each node's full
 * <b>descriptor-fiber</b> — every grid that routes there, including ones never seen — and reports how the
 * Plexus would label them. The context is a sample; the fiber is the truth. So each row exposes the gap:
 * how many of the possibilities were actually in the data ({@code seen}), and how the router splits the
 * whole fiber by predicted label.
 *
 * <p>Same walk as {@link TopologyReport}, different node-function: the traversal lives in {@link Topology}.
 */
public class PossibilityReport implements Report {

    private static final int MAX_MEMBERS = 8;

    private final Set<String> seen;
    private final int side;

    public PossibilityReport(List<int[]> inputs) {
        this.seen = inputs.stream().map(Arrays::toString).collect(Collectors.toSet());
        this.side = inputs.isEmpty() ? 0 : (int) Math.round(Math.sqrt(inputs.get(0).length));
    }

    @Override
    public String apply(Plexus plexus) {
        final var sb = new StringBuilder();
        sb.append("Possibility space - every fixed symmetry-break and its full descriptor-fiber\n");
        sb.append("(fiber = all grids that route here incl. unseen; seen = how many were in training)\n");
        new Topology(node -> render(plexus, sb, node)).apply(plexus);
        return sb.toString();
    }

    private void render(Plexus plexus, StringBuilder sb, Topology.Node node) {
        if (node.radial()) return;
        if (node.groupBoundary()) sb.append(String.format("%n[%s]%n", node.symmetryLabel()));

        final var fiber = new Fiber(node.key(), side).apply(plexus).toList();
        final var seenCount = fiber.stream().filter(g -> seen.contains(Arrays.toString(g))).count();
        final var asTarget = fiber.stream().filter(g -> plexus.score(g) == 1).toList();
        final var asReject = fiber.size() - asTarget.size();
        final var pct = fiber.isEmpty() ? 0 : 100 * seenCount / fiber.size();
        final var kind = node.context().isTarget() ? "TARGET" : "reject";
        final var broken = node.brokenSymmetries().isEmpty() ? ""
                : " BROKEN[" + String.join(", ", node.brokenSymmetries()) + "]";

        sb.append(String.format("  |- %-18s %s  fiber=%-3d seen=%d/%d (%d%%)  predict 1:%d 0:%d%s%n",
                node.coords(), kind, fiber.size(), seenCount, fiber.size(), pct,
                asTarget.size(), asReject, broken));
        if (!asTarget.isEmpty())
            sb.append("        predicted target: ").append(renderMembers(asTarget)).append('\n');
    }

    /** Compactly render up to {@link #MAX_MEMBERS} grids as {@code rows/of/x.} forms. */
    private String renderMembers(List<int[]> grids) {
        final var shown = grids.size() <= MAX_MEMBERS ? grids : grids.subList(0, MAX_MEMBERS);
        final var parts = shown.stream().map(this::render).toList();
        return String.join("  ", parts) + (grids.size() > MAX_MEMBERS ? "  …" : "");
    }

    private String render(int[] g) {
        final var sb = new StringBuilder();
        for (var r = 0; r < side; r++) {
            for (var c = 0; c < side; c++) sb.append(g[r * side + c] == 1 ? 'x' : '.');
            if (r < side - 1) sb.append('/');
        }
        return sb.toString();
    }
}
