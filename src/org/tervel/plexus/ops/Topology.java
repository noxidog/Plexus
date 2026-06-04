package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.Plexus.Context;
import org.tervel.plexus.Plexus.Key;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The outer automaton over the key trie. It walks every context in canonical trie order — by symmetry
 * verdict (radial last), then stabilizer, then discriminator coordinates — and at each node <b>reads the
 * context</b> the symmetry-break addresses, handing the pair to an injected {@link Consumer}. The walk is
 * the reusable engine; <b>what happens at a node</b> — print it, generate its fiber, tally it — is the
 * consumer's choice. {@link TopologyReport} is simply the printing consumer; other consumers can turn the
 * same traversal into a possibility-space query, a histogram, an export.
 */
public final class Topology implements Operation<Void> {

    /**
     * Everything available at one node: the symmetry-break {@code key}, the {@code context} it reads, the
     * owning {@code plexus}, and whether this node opens a new symmetry group in the walk. Convenience
     * accessors expose the information a consumer usually wants without re-deriving it.
     */
    public record Node(Plexus plexus, Key key, Context context, boolean groupBoundary) {

        public boolean radial()        { return key.radial(); }
        public String  symmetryLabel() { return plexus.group().describe(key.symmetry()); }
        public String  coords()        { return plexus.coordString(key); }

        /** Broken symmetries: transforms carrying an exception onto a majority-class grid (empty if pure). */
        public List<String> brokenSymmetries() {
            if (context.exceptionCount() == 0) return List.of();
            final var names = new ArrayList<String>();
            new Searcher(context.exceptions().get(0), context.answer()).apply(plexus)
                    .forEachRemaining(t -> names.add(t.label()));
            return names;
        }
    }

    private final Consumer<Node> visit;

    public Topology(Consumer<Node> visit) { this.visit = visit; }

    @Override
    public Void apply(Plexus plexus) {
        final var contexts = plexus.contexts();
        final var keys = contexts.keySet().stream()
                .sorted(Comparator.comparing(Key::radial)
                        .thenComparingInt(Key::symmetry)
                        .thenComparing(k -> k.coords().toString()))
                .toList();

        Boolean lastRadial = null;
        var lastSym = Integer.MIN_VALUE;
        for (final var k : keys) {
            final var boundary = lastRadial == null || k.radial() != lastRadial || k.symmetry() != lastSym;
            visit.accept(new Node(plexus, k, contexts.get(k), boundary));
            lastRadial = k.radial();
            lastSym = k.symmetry();
        }
        return null;
    }
}
