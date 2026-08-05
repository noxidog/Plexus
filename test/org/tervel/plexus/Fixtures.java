package org.tervel.plexus;

import org.tervel.plexus.invariants.Connectivity;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.invariants.Mass;
import org.tervel.plexus.invariants.Signature;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.symmetry.Dihedral;
import org.tervel.plexus.symmetry.SymmetryGroup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared dataset wiring for the suite — the same frame/group/chain decisions {@code Main} makes, so a test
 * exercises the shipped configuration rather than an idealised one.
 */
final class Fixtures {

    /** Every reference dataset, by file stem. */
    static final List<String> ALL = List.of(
            "conservation-t-orbit-3x3", "twin-t-flip-3x3", "position-corner-dot-3x3",
            "separable-path-fork-5x5", "entropy-fiber-gap-5x5", "renormalization-path-fork-9x9",
            "rect-bar-2x4");

    /** The square-framed datasets — the geometry-heavy ops and the Primitive battery assume a square. */
    static final List<String> SQUARE = ALL.stream().filter(n -> !n.equals("rect-bar-2x4")).toList();

    record Data(List<int[]> grids, List<Integer> labels, int rows, int cols,
                SymmetryGroup group, List<Invariant> chain) {
        boolean square() { return rows == cols; }

        /** A router trained on this dataset, wired exactly as {@code Main} wires it. */
        Plexus trained() {
            final var p = new Plexus(group, chain, new ExactExceptions(), rows, cols);
            p.fit(grids, labels);
            return p;
        }

        /** An untrained router — for tests that drive {@code accept} themselves. */
        Plexus empty() { return new Plexus(group, chain, new ExactExceptions(), rows, cols); }
    }

    /**
     * Load a reference dataset. The parsing is deliberately <b>independent</b> of the CLI's parser rather
     * than delegating to it: a suite that builds its inputs with the same code it is testing cannot see a
     * parser defect, because the defect would be applied to both sides. The format is small enough that a
     * second reading of it is cheap insurance.
     */
    static Data load(String stem) {
        final var file = Path.of("data", stem + ".txt");
        try {
            final var lines = Files.readAllLines(file);
            final var rows = Integer.parseInt(lines.get(0).split("x")[0].trim());
            final var body = lines.subList(1, lines.size());
            final var grids = new ArrayList<int[]>();
            final var labels = new ArrayList<Integer>();
            for (var i = 0; i + rows < body.size(); i += rows + 1) {
                grids.add(body.subList(i, i + rows).stream()
                        .flatMapToInt(String::chars).map(c -> c == 'x' ? 1 : 0).toArray());
                labels.add(Integer.parseInt(body.get(i + rows).trim()));
            }
            final var cols = grids.get(0).length / Math.max(1, rows);
            final var square = rows == cols;
            final var group = square ? Dihedral.d4() : Dihedral.d2();
            final var connectivity = square ? new Connectivity() : new Connectivity(rows, cols);
            final List<Invariant> chain =
                    List.of(new Signature(new Mass(), connectivity, square ? rows : rows + cols));
            return new Data(List.copyOf(grids), List.copyOf(labels), rows, cols, group, chain);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot load " + file, e);
        }
    }

    private Fixtures() { }
}
