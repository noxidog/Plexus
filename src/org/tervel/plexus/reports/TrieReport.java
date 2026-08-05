package org.tervel.plexus.reports;

import org.tervel.plexus.trie.Trie;

import java.util.ArrayList;
import java.util.List;

/**
 * The two readings of a {@link Trie}'s shape. They are here rather than on the structure because they are
 * <b>formatting</b> — every line is a {@code StringBuilder} append, and the package that owns that is this
 * one. The trie exposes {@link Trie.NodeView} so a report can walk it without the structure leaking its
 * nodes.
 *
 * <p>Unlike the other reports these are not {@link Report}s: a {@code Report} is a function of a trained
 * {@code Plexus}, and a trie is a different subject. The shape is the same, the input is not.
 */
public final class TrieReport {

    /**
     * <b>Census</b> — nodes per rung, how many are already label-pure, and how many are <b>redundant</b>:
     * their parent was pure, so that rung's coordinate decided nothing for them and a radix merge would
     * collapse them upward. The redundant column is the compression the prefixes expose.
     *
     * <p>Rung names come from the declared vocabulary, so under a per-sector chain (§4) or after extension
     * they are the union rather than a per-node guarantee.
     */
    public static String census(Trie trie) {
        final var sb = new StringBuilder(
                "trie census — nodes per rung (pure = one label; redundant = parent already pure)\n");
        for (final var radial : new boolean[]{false, true}) {
            final var rows = new ArrayList<long[]>();
            final var total = new long[1];
            trie.forEachNode(n -> {
                if (n.radial() != radial) return;
                if (n.depth() == 0) total[0] = n.total();
                while (rows.size() <= n.depth()) rows.add(new long[3]);
                final var row = rows.get(n.depth());
                row[0]++;
                if (n.pure()) row[1]++;
                if (n.parentPure()) row[2]++;
            });
            if (rows.isEmpty()) continue;
            final var names = trie.rungs(radial);
            sb.append(String.format("  %s track  (%d examples)%n", radial ? "radial" : "oriented", total[0]));
            sb.append(String.format("    %-6s %-16s %7s %7s %10s%n",
                    "depth", "rung", "nodes", "pure", "redundant"));
            for (var d = 0; d < rows.size(); d++) {
                final var r = rows.get(d);
                sb.append(String.format("    %-6d %-16s %7d %7d %10d%n",
                        d, d < names.size() ? names.get(d) : "?", r[0], r[1], r[2]));
            }
        }
        return sb.toString();
    }

    /** <b>Structure</b> — the grown tree: what each node split on, and what each leaf ended up holding. */
    public static String structure(Trie trie) {
        final var sb = new StringBuilder("trie — extension chosen at insert, memory only where forced\n");
        final var seen = new ArrayList<Boolean>();
        trie.forEachNode(n -> {
            if (!seen.contains(n.radial())) {
                seen.add(n.radial());
                sb.append(String.format("  %s track (%d examples)%n",
                        n.radial() ? "radial" : "oriented", n.total()));
            }
            sb.append("    ").append("  ".repeat(n.depth()))
              .append(String.format("%s  [%d neg, %d pos]%s%n", n.edge(), n.neg(), n.pos(), note(n)));
        });
        sb.append(String.format("  %d local extension(s); %d row(s) stored, %d on an irreducible leaf%n",
                trie.splits(), trie.storedRows(), trie.irreducibleLeaves()));
        return sb.toString();
    }

    private static String note(Trie.NodeView n) {
        if (!n.leaf()) return "  split on '" + n.splitOn() + "'";
        if (n.pure()) return "  leaf, pure (" + n.rows() + " row(s))";
        if (n.irreducible()) return "  leaf, IRREDUCIBLE — one orbit carries both labels (provable twin)";
        return "  leaf, " + n.rows() + " row(s), no column separates — exact per stored member";
    }

    /** The distinct tracks a trie populated, for a caller that wants them without formatting. */
    static List<Boolean> tracks(Trie trie) {
        final var out = new ArrayList<Boolean>();
        trie.forEachNode(n -> { if (n.depth() == 0 && !out.contains(n.radial())) out.add(n.radial()); });
        return out;
    }

    private TrieReport() { }
}
