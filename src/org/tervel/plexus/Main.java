package org.tervel.plexus;

import org.tervel.plexus.diagnostics.CollisionDiagnostic;
import org.tervel.plexus.invariants.Connectivity;
import org.tervel.plexus.invariants.Invariant;
import org.tervel.plexus.invariants.Mass;
import org.tervel.plexus.invariants.Signature;
import org.tervel.plexus.residual.ExactExceptions;
import org.tervel.plexus.residual.ResidualResolver;
import org.tervel.plexus.ops.Decompose;
import org.tervel.plexus.ops.Scale;
import org.tervel.plexus.reports.GridQuery;
import org.tervel.plexus.reports.PossibilityReport;
import org.tervel.plexus.reports.ScoreReport;
import org.tervel.plexus.reports.SliceReport;
import org.tervel.plexus.reports.StabilizerQuery;
import org.tervel.plexus.reports.TopologyReport;
import org.tervel.plexus.reports.VolumeReport;
import org.tervel.plexus.symmetry.Dihedral;
import org.tervel.plexus.symmetry.SymmetryGroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

public class Main {

    /** One labelled example, kept together — no splitting the dataset by class. */
    public record Example(int[] grid, int label) { }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: Main <data-file>");
            return;
        }
        final var data = parse(Path.of(args[0]));
        // Domain frame from the data's "nxm" header. A square domain (rows == cols) routes under the full
        // square group D4; a rectangular domain breaks the transpose and routes under D2 (§11, "the
        // rectangle"; org.tervel.plexus.symmetry.Crystallographic — order 2 still covers the plane).
        final var rows = data.isEmpty() ? 0 : Integer.parseInt(Files.readAllLines(Path.of(args[0])).get(0).split("x")[0].trim());
        final var cols = data.isEmpty() ? 0 : data.get(0).grid().length / Math.max(1, rows);
        final var square = rows == cols;
        final var group = square ? Dihedral.d4() : Dihedral.d2();
        // Compose mass (V) and connectivity (E) into one Signature column: both are group-invariant (every
        // rigid transform preserves cell and adjacency counts), so each is a legal symmetry coordinate —
        // but blind alone, so they're packed together. Connectivity carries the frame so adjacency uses the
        // true row stride; the Signature packing base just needs to exceed the max edge count, which
        // width(rows + cols) does for any rectangle. Opt-in axes (held out by minimality, §3): add
        // new Position() (radial-only) for dot cases, or new MaxDegree() to separate a path from a fork.
        final var connectivity = square ? new Connectivity() : new Connectivity(rows, cols);
        final var invariants = List.<Invariant>of(new Signature(new Mass(), connectivity, square ? rows : rows + cols));
        // The residual policy past the twin floor (§9.4): the deterministic exact-exception store. Swap in a
        // brute-force or learned ResidualResolver here to predict unseen twins instead of memorising them.
        final var resolver = new ExactExceptions();

        final var plexus = train(group, invariants, resolver, data, rows, cols);
        final var inputs = data.stream().map(Example::grid).toList();
        final var labels = data.stream().map(Example::label).toList();

        // Training post-pass: surface any separable collision as a blueprint for the next invariant.
        System.out.println(new CollisionDiagnostic(inputs, labels).apply(plexus));

        repl(plexus, inputs, labels);
    }

    /** Trains a Plexus on the dataset (square domain — frame inferred per grid). */
    static Plexus train(SymmetryGroup group, List<Invariant> invariants, ResidualResolver resolver, List<Example> data) {
        return train(group, invariants, resolver, data, -1, -1);
    }

    /**
     * Trains a Plexus on the dataset under the given group, chain, resolver, and explicit {@code rows×cols}
     * domain frame ({@code rows, cols <= 0} = infer a square frame per grid).
     */
    static Plexus train(SymmetryGroup group, List<Invariant> invariants, ResidualResolver resolver,
                        List<Example> data, int rows, int cols) {
        final var plexus = new Plexus(group, invariants, resolver, rows, cols);
        plexus.fit(data.stream().map(Example::grid).toList(),
                   data.stream().map(Example::label).toList());
        return plexus;
    }

    /** Interactive interpreter: query a node by one of the three coordinate kinds, then the whole-table reports. */
    static void repl(Plexus plexus, List<int[]> inputs, List<Integer> labels) {
        final var in = new Scanner(System.in);
        final var side = inputs.isEmpty() ? 0 : (int) Math.round(Math.sqrt(inputs.get(0).length));
        while (true) {
            System.out.println("""

                    Menu  -  the descriptor read both directions (classify/generate), at two scopes:

                                   a node                                  the whole table
                      resolve  ->  [1] stabilizer  [4] grid                [5] score
                      generate <-  [2] invariant   [3] label  [9] atoms  [s] scale    [6] topology  [7] possibility  [v] volume  [8] diagnose
                      [q] quit""");
            System.out.print("> ");
            if (!in.hasNextLine()) return;
            final var cmd = in.nextLine().trim().toLowerCase();
            switch (cmd) {
                case "1" -> queryStabilizer(plexus, in, side, inputs, labels);
                case "2" -> queryInvariant(plexus, in);
                case "3" -> queryLabel(plexus, in);
                case "4" -> queryGrid(plexus, in, side);
                case "5" -> System.out.println(new ScoreReport(inputs, labels).apply(plexus));
                case "6" -> System.out.println(new TopologyReport().apply(plexus));
                case "7" -> System.out.println(new PossibilityReport(inputs).apply(plexus));
                case "v" -> System.out.println(new VolumeReport(side).apply(plexus));
                case "8" -> System.out.println(new CollisionDiagnostic(inputs, labels).apply(plexus));
                case "9" -> queryDecompose(plexus, in, side);
                case "s" -> queryScale(plexus, in, side);
                case "q", "quit", "exit" -> { return; }
                case "" -> { }
                default -> System.out.println("unknown command: " + cmd);
            }
        }
    }

    /**
     * [1] Query by stabilizer. Type allowed symmetries to query under an explicit stabilizer (posed mode);
     * leave them blank and give a label to <em>solve</em> the stabilizer from that label (gather = expand to
     * maximal recall, separate = shrink to the shape's own); leave both blank for the shape's own stabilizer.
     */
    static void queryStabilizer(Plexus plexus, Scanner in, int side, List<int[]> grids, List<Integer> labels) {
        try {
            System.out.print("canonical shape (e.g. xxx/.x.):  ");
            final var grid = parseGrid(in.nextLine(), side);
            System.out.print("allowed symmetries (e.g. mirrorV; blank = solve from a label):  ");
            final var line = in.nextLine().trim();
            if (!line.isEmpty()) {
                System.out.println("\n" + new StabilizerQuery(grid, maskOf(plexus.group(), line)).apply(plexus));
                return;
            }
            System.out.print("label to solve for (1/0; blank = the shape's own stabilizer):  ");
            final var labelLine = in.nextLine().trim();
            if (labelLine.isEmpty()) {
                System.out.println("\n" + new StabilizerQuery(grid, selfStabilizer(plexus, grid)).apply(plexus));
                return;
            }
            System.out.print("direction (gather = expand / separate = shrink; blank = gather):  ");
            final var gather = !in.nextLine().trim().toLowerCase().startsWith("s");
            System.out.println("\n" + new StabilizerQuery(grid, Integer.parseInt(labelLine), gather, grids, labels).apply(plexus));
        } catch (RuntimeException e) {
            System.out.println("bad query: " + e.getMessage());
        }
    }

    /** [4] Query by grid: the exact typed placement (no centering) — resolve its label and verdict. */
    static void queryGrid(Plexus plexus, Scanner in, int side) {
        try {
            System.out.print("exact grid placement (e.g. .../.x./xxx):  ");
            final var grid = parseGrid(in.nextLine(), side);
            System.out.println("\n" + new GridQuery(grid).apply(plexus));
        } catch (RuntimeException e) {
            System.out.println("bad query: " + e.getMessage());
        }
    }

    /** [9] Decompose: factor a typed shape into atoms under a cut-law (straight = bars, bent = Ls). */
    static void queryDecompose(Plexus plexus, Scanner in, int side) {
        try {
            System.out.print("shape to decompose (e.g. xxx/.x.):  ");
            final var grid = parseGrid(in.nextLine(), side);
            System.out.print("cut law (straight = bars / bent = Ls; blank = straight):  ");
            final var law = in.nextLine().trim().toLowerCase().startsWith("b")
                    ? Decompose.CutLaw.BENT : Decompose.CutLaw.STRAIGHT;
            System.out.println("\n" + new Decompose(grid, law).apply(plexus).render());
        } catch (RuntimeException e) {
            System.out.println("bad query: " + e.getMessage());
        }
    }

    /** [s] Scale: coarse-grain a typed grid by k×k block-decimation (the renormalization direction, §11). */
    static void queryScale(Plexus plexus, Scanner in, int side) {
        try {
            System.out.print("grid to coarse-grain (e.g. xxx/.x.):  ");
            final var grid = parseGrid(in.nextLine(), side);
            System.out.print("block size k (must divide " + side + "; blank = 2):  ");
            final var kLine = in.nextLine().trim();
            final var k = kLine.isEmpty() ? 2 : Integer.parseInt(kLine);
            System.out.print("rule (canonical / occupancy / majority; blank = canonical):  ");
            final var r = in.nextLine().trim().toLowerCase();
            final var rule = r.startsWith("m") ? Scale.Rule.MAJORITY
                    : r.startsWith("o") ? Scale.Rule.OCCUPANCY : Scale.Rule.CANONICAL;
            System.out.println("\n" + new Scale(grid, k, rule).apply(plexus).render());
        } catch (RuntimeException e) {
            System.out.println("bad query: " + e.getMessage());
        }
    }

    /** [2] Query by invariant: list contexts whose named coordinate matches, e.g. {@code mass=4}. */
    static void queryInvariant(Plexus plexus, Scanner in) {
        try {
            System.out.print("invariant filter (e.g. mass=4):  ");
            final var parts = in.nextLine().split("=", 2);
            final var col = parts[0].trim();
            final var val = Integer.parseInt(parts[1].trim());
            System.out.println("\n" + new SliceReport("contexts with " + col + "=" + val,
                    node -> { final var v = plexus.namedCoords(node.key()).get(col); return v != null && v == val; }).apply(plexus));
        } catch (RuntimeException e) {
            System.out.println("bad query: " + e.getMessage());
        }
    }

    /** [3] Query by label: list contexts whose answer is the requested label (1=TARGET, 0=reject). */
    static void queryLabel(Plexus plexus, Scanner in) {
        try {
            System.out.print("label (1=TARGET, 0=reject):  ");
            final var want = Integer.parseInt(in.nextLine().trim());
            System.out.println("\n" + new SliceReport("contexts answering " + want,
                    node -> node.context().answer() == want).apply(plexus));
        } catch (RuntimeException e) {
            System.out.println("bad query: " + e.getMessage());
        }
    }

    /** The shape's own stabilizer in its centred frame — the default "allowed symmetries". */
    static int selfStabilizer(Plexus plexus, int[] grid) {
        final var c = Plexus.center(grid);
        return c == null ? 1 : plexus.group().stabilizer(c);
    }

    /** Parse a comma-separated symmetry list to a stabilizer bitmask; identity is always included. */
    static int maskOf(SymmetryGroup group, String s) {
        final var ts = group.transforms();
        var mask = 1;                                                  // identity (bit 0) always present
        for (final var part : s.split(",")) {
            final var name = part.trim();
            if (name.isEmpty()) continue;
            var found = false;
            for (var i = 0; i < ts.size(); i++) if (ts.get(i).label().equalsIgnoreCase(name)) { mask |= 1 << i; found = true; }
            if (!found) throw new IllegalArgumentException("unknown symmetry: " + name);
        }
        return mask;
    }

    /**
     * Parses a grid in the object's own terms and pads it into the {@code side×side} frame at top-left.
     * Rows are separated by {@code '/'} ({@code 'x'}/{@code '1'} on, anything else off); trailing empty
     * rows and short rows need not be written, so {@code "xxx/.x."} is the 2×3 object, not a padded 3×3.
     * A slashless string is chunked into rows of width {@code side}. The shape must fit the frame.
     */
    static int[] parseGrid(String s, int side) {
        final var clean = s.replaceAll("\\s", "");
        final var rows = new ArrayList<String>();
        if (clean.contains("/")) {
            for (final var part : clean.split("/", -1)) rows.add(part);
        } else {
            for (var i = 0; i < clean.length(); i += side) rows.add(clean.substring(i, Math.min(clean.length(), i + side)));
        }
        final var h = rows.size();
        final var w = rows.stream().mapToInt(String::length).max().orElse(0);
        if (h > side || w > side)
            throw new IllegalArgumentException("shape " + h + "x" + w + " is larger than the " + side + "x" + side + " frame");
        final var g = new int[side * side];
        for (var r = 0; r < h; r++) {
            final var row = rows.get(r);
            for (var c = 0; c < row.length(); c++) {
                final var ch = row.charAt(c);
                if (ch == 'x' || ch == 'X' || ch == '1') g[r * side + c] = 1;
            }
        }
        return g;
    }

    /**
     * Parses the dataset into one list of labelled examples, kept in file order.
     * The first line is the {@code nxm} dimensions; the remainder are records of
     * {@code n} grid rows ('_' empty, 'x' occupied) followed by a 0/1 label.
     */
    static List<Example> parse(Path file) throws IOException {
        final var lines = Files.readAllLines(file);
        final var rows = Integer.parseInt(lines.get(0).split("x")[0]);
        final var stride = rows + 1;
        final var body = lines.subList(1, lines.size());

        final var out = new ArrayList<Example>();
        IntStream.iterate(0, i -> i < body.size(), i -> i + stride)
                .forEach(i -> {
                    final var cells = body.subList(i, i + rows).stream()
                            .flatMapToInt(String::chars)
                            .map(ch -> ch == 'x' ? 1 : 0)
                            .toArray();
                    final var label = Integer.parseInt(body.get(i + rows).trim());
                    out.add(new Example(cells, label));
                });
        return out;
    }
}
