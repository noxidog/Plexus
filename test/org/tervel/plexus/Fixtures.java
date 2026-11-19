package org.tervel.plexus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The reference datasets, loaded independently of the CLI's parser so a parser defect cannot hide in both. */
final class Fixtures {

    /** Every reference dataset, by file stem. */
    static final List<String> ALL = List.of(
            "conservation-t-orbit-3x3", "twin-t-flip-3x3", "position-corner-dot-3x3",
            "separable-path-fork-5x5", "entropy-fiber-gap-5x5", "renormalization-path-fork-9x9",
            "rect-bar-2x4");

    record Data(List<int[]> grids, List<Integer> labels, int rows, int cols) {
        boolean square() { return rows == cols; }

        /** A router trained on this dataset. */
        Plexus trained() {
            final var p = new Plexus(rows, cols);
            p.fit(grids, labels);
            return p;
        }
    }

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
            return new Data(List.copyOf(grids), List.copyOf(labels), rows, cols);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot load " + file, e);
        }
    }

    private Fixtures() { }
}
