package org.tervel.plexus.cli;

import org.tervel.plexus.Descriptor;
import org.tervel.plexus.Extrapolate;
import org.tervel.plexus.Lattice;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.Root;
import org.tervel.plexus.Slice;
import org.tervel.plexus.Verdict;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * The interpreter: train a {@link Plexus} on a data file, then query it.
 *
 * <pre>
 *   [5] score     how many training grids read back with their own label
 *   [t] trie      both tries (D4, D6), drawn as paths with counts
 *   [4] grid      a typed grid's descriptor, and its verdict under every setting
 *   [s] slice     walk a slice and print what it produces
 *   [8] diagnose  the mixed junctions and leaves, and what resolves each
 *   [q] quit
 * </pre>
 *
 * <p>A data file starts with an {@code nxm} line, then records of {@code n} grid rows ({@code x} filled, anything
 * else empty) each followed by a {@code 0} or {@code 1} label. Only {@code n} is read from the first line; the
 * column count is the first record's cell count divided by {@code n}.
 *
 * <p>{@code --lattice} picks the router's lattices, comma separated: {@code square4}, {@code square8},
 * {@code hex6} (default {@code square4,hex6}). Exactly one square lattice; hex is optional.
 */
public final class Main {

    /** One labelled example. */
    public record Example(int[] grid, int label) { }

    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        String file = null;
        var lattices = Lattice.DEFAULT;
        try {
            for (var i = 0; i < args.length; i++) {
                if (args[i].equals("--lattice") && i + 1 < args.length) lattices = lattices(args[++i]);
                else if (args[i].startsWith("--lattice=")) lattices = lattices(args[i].substring("--lattice=".length()));
                else if (file == null && !args[i].startsWith("--")) file = args[i];
                else throw new IllegalArgumentException("unexpected argument: " + args[i]);
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            file = null;
        }
        if (file == null) {
            System.err.println("usage: Main <data-file> [--lattice square4,hex6 | square8 | square8,hex6 | square4]");
            return;
        }
        final var path = Path.of(file);
        final var rows = Integer.parseInt(Files.readAllLines(path).get(0).split("x")[0].trim());
        final var data = parse(path);
        final var cols = data.isEmpty() ? 0 : data.get(0).grid().length / Math.max(1, rows);
        final var plexus = new Plexus(rows, cols, lattices, true);
        plexus.fit(data.stream().map(Example::grid).toList(), data.stream().map(Example::label).toList());
        System.out.printf("plexus: %d example(s) in a %dx%d frame, lattices %s, %d node(s), extensions %s%n",
                data.size(), rows, cols, plexus.lattices().stream().sorted().toList(), plexus.size(), plexus.extensions().isEmpty() ? "none" : plexus.extensions());
        repl(plexus, data, rows, cols);
    }

    /** Parse a {@code --lattice} value such as {@code square8,hex6}. */
    static java.util.Set<Lattice> lattices(String spec) {
        final var out = EnumSet.noneOf(Lattice.class);
        for (final var part : spec.split(",")) {
            final var t = part.trim().toLowerCase(Locale.ROOT);
            out.add(switch (t) {
                case "square4" -> Lattice.SQUARE_4;
                case "square8" -> Lattice.SQUARE_8;
                case "hex6", "hex" -> Lattice.HEX_6;
                default -> throw new IllegalArgumentException("unknown lattice: " + t + " (square4, square8, hex6)");
            });
        }
        return Lattice.checked(out);
    }

    static void repl(Plexus plexus, List<Example> data, int rows, int cols) {
        final var in = new Scanner(System.in);
        while (true) {
            System.out.println("\n[5] score  [t] trie  [4] grid  [s] slice  [8] diagnose  [q] quit");
            System.out.print("> ");
            if (!in.hasNextLine()) return;
            final var cmd = in.nextLine().trim().toLowerCase(Locale.ROOT);
            try {
                switch (cmd) {
                    case "5" -> score(plexus, data);
                    case "t" -> System.out.print(plexus.render());
                    case "4" -> {
                        System.out.print("grid (e.g. xxx/.x.):  ");
                        grid(plexus, parseGrid(in.nextLine(), rows, cols), rows, cols);
                    }
                    case "s" -> slice(plexus, in, rows, cols);
                    case "8" -> System.out.print(plexus.diagnose());
                    case "q", "quit", "exit" -> { return; }
                    case "" -> { }
                    default -> System.out.println("unknown command: " + cmd);
                }
            } catch (RuntimeException e) {
                System.out.println("bad query: " + e.getMessage());
            }
        }
    }

    private static void score(Plexus plexus, List<Example> data) {
        var right = 0;
        for (final var e : data) if (plexus.read(e.grid()).label() == e.label()) right++;
        System.out.printf("read back %d/%d training grid(s) with their own label%n", right, data.size());
    }

    private static void grid(Plexus plexus, int[] g, int rows, int cols) {
        render(g, rows, cols, "    ");
        final Descriptor d = plexus.describe(g);
        System.out.printf("  %s on %s, box %dx%d, symmetries %s, mass %d, joins %d, orientation %d, pose %d, corner (%d,%d)%n",
                d.kind(), d.root(), d.boxShort(), d.boxLong(), d.symmetries(), d.mass(), d.joins(),
                d.orientation(), d.pose(), d.cornerRow(), d.cornerCol());
        for (final var extrapolate : Extrapolate.values()) {
            final var a = plexus.read(g, extrapolate);
            System.out.printf("  %-10s %-8s%s   %s%n", extrapolate, a.verdict(),
                    a.verdict() == Verdict.UNSEEN ? "" : " label " + a.label(), a.context());
        }
    }

    private static void slice(Plexus plexus, Scanner in, int rows, int cols) {
        System.out.print("root (d4 / d6 / both; blank = both):  ");
        final var r = in.nextLine().trim().toLowerCase(Locale.ROOT);
        if (!blank(r) && !r.equals("d4") && !r.equals("d6"))
            throw new IllegalArgumentException("root must be d4, d6 or both, not \"" + r + "\"");
        final var q = Slice.on(r.equals("d4") ? Root.D4 : r.equals("d6") ? Root.D6 : Root.BOTH);
        System.out.print("at least these symmetries (comma-separated, e.g. rot180, short-axis mirror; blank = none):  ");
        final var sym = in.nextLine().trim();
        if (!blank(sym)) q.atLeast(sym.split("\\s*,\\s*"));
        System.out.print("mass (a number or a range like 4-5; blank = any):  ");
        final var m = range(in.nextLine(), "mass");
        if (m != null) q.mass(m[0], m[1]);
        System.out.print("joins (a number or a range like 3-4; blank = any):  ");
        final var j = range(in.nextLine(), "joins");
        if (j != null) q.joins(j[0], j[1]);
        System.out.print("label (1 / 0; blank = both):  ");
        final var l = in.nextLine().trim();
        if (!blank(l)) {
            if (!l.equals("0") && !l.equals("1")) throw new IllegalArgumentException("label must be 1 or 0, not \"" + l + "\"");
            q.label(Integer.parseInt(l));
        }
        System.out.print("orientation (0 / 1, or upright / turned on a rectangle, right / left on hex; blank = any):  ");
        final var o = in.nextLine().trim().toLowerCase(Locale.ROOT);
        if (!blank(o)) {
            if (o.equals("0") || o.equals("upright") || o.equals("right")) q.orientation(0);
            else if (o.equals("1") || o.equals("turned") || o.equals("left")) q.orientation(1);
            else throw new IllegalArgumentException("orientation must be 0 or 1 (upright / turned, right / left), not \"" + o + "\"");
        }
        System.out.print("pose (a number, 0 = the canonical pose; blank = any):  ");
        final var po = in.nextLine().trim();
        if (!blank(po)) {
            try { q.pose(Integer.parseInt(po)); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("pose must be a number, not \"" + po + "\""); }
        }
        System.out.print("corner (row,col of the box's top-left; blank = any):  ");
        final var co = in.nextLine().trim();
        if (!blank(co)) {
            final var rc = co.split("\\s*,\\s*");
            try {
                if (rc.length != 2) throw new NumberFormatException();
                q.corner(Integer.parseInt(rc[0]), Integer.parseInt(rc[1]));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("corner must be row,col like 0,1, not \"" + co + "\"");
            }
        }
        System.out.print("show one entry per (placement / pose / shape; blank = placement):  ");
        final var per = in.nextLine().trim().toLowerCase(Locale.ROOT);
        if (per.isEmpty() || per.startsWith("pl")) q.per(Slice.Per.PLACEMENT);
        else if (per.startsWith("po")) q.per(Slice.Per.POSE);
        else if (per.startsWith("s")) q.per(Slice.Per.SHAPE);
        else throw new IllegalArgumentException("show must be placement, pose or shape, not \"" + per + "\"");
        final var unit = per.startsWith("po") ? " pose(s)" : per.startsWith("s") ? " shape(s)" : "";
        System.out.print("extrapolate (none / placement / symmetry / everything; blank = symmetry):  ");
        final var ex = in.nextLine().trim().toLowerCase(Locale.ROOT);
        if (ex.isEmpty() || ex.startsWith("s")) q.extrapolate(Extrapolate.SYMMETRY);
        else if (ex.startsWith("n")) q.extrapolate(Extrapolate.NONE);
        else if (ex.startsWith("p")) q.extrapolate(Extrapolate.PLACEMENT);
        else if (ex.startsWith("e")) q.extrapolate(Extrapolate.EVERYTHING);
        else throw new IllegalArgumentException("extrapolate must be none, placement, symmetry or everything, not \"" + ex + "\"");
        final var result = plexus.slice(q, (g, context, evidence) -> {
            System.out.println(evidence + "  label " + context.answer() + "  " + context);
            render(g, rows, cols, "    ");
        });
        System.out.printf("%d confirmed%s, %d assumed%s%n", result.confirmed(), unit, result.assumed(),
                result.skipped() > 0 ? ", " + result.skipped() + " node(s) too large to generate" : "");
    }

    /** Whether an answer means "no constraint": empty, or one of the words any, all, both, none. */
    private static boolean blank(String s) {
        final var t = s.trim().toLowerCase(Locale.ROOT);
        return t.isEmpty() || t.equals("any") || t.equals("all") || t.equals("both") || t.equals("none");
    }

    /** A typed number ({@code 5}) or range ({@code 4-5}) as {@code {min, max}}; null for no constraint. */
    static int[] range(String typed, String what) {
        if (blank(typed)) return null;
        final var parts = typed.trim().split("\\s*-\\s*");
        try {
            if (parts.length == 1) { final var v = Integer.parseInt(parts[0]); return new int[]{v, v}; }
            if (parts.length == 2) return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            // falls through to the message below
        }
        throw new IllegalArgumentException(what + " must be a number or a range like 4-5, not \"" + typed.trim() + "\"");
    }

    private static void render(int[] g, int rows, int cols, String indent) {
        for (var r = 0; r < rows; r++) {
            final var sb = new StringBuilder(indent);
            for (var c = 0; c < cols; c++) sb.append(g[r * cols + c] == 1 ? 'x' : '.');
            System.out.println(sb);
        }
    }

    // ---- parsing ---------------------------------------------------------------------------------------

    /** The labelled examples of a data file, in file order. */
    static List<Example> parse(Path file) throws IOException {
        final var lines = Files.readAllLines(file);
        final var rows = Integer.parseInt(lines.get(0).split("x")[0].trim());
        final var body = lines.subList(1, lines.size());
        final var out = new ArrayList<Example>();
        for (var i = 0; i + rows < body.size(); i += rows + 1) {
            final var cells = body.subList(i, i + rows).stream().flatMapToInt(String::chars).map(ch -> ch == 'x' ? 1 : 0).toArray();
            out.add(new Example(cells, Integer.parseInt(body.get(i + rows).trim())));
        }
        return out;
    }

    /**
     * A typed grid in the frame's own terms, placed at the top-left: rows separated by {@code '/'}, {@code 'x'}
     * or {@code '1'} filled, anything else empty. A slashless string is chunked into rows of the frame's width.
     */
    static int[] parseGrid(String s, int frameRows, int frameCols) {
        final var clean = s.replaceAll("\\s", "");
        final var lines = new ArrayList<String>();
        if (clean.contains("/")) lines.addAll(List.of(clean.split("/", -1)));
        else for (var i = 0; i < clean.length(); i += frameCols) lines.add(clean.substring(i, Math.min(clean.length(), i + frameCols)));
        final var h = lines.size();
        final var w = lines.stream().mapToInt(String::length).max().orElse(0);
        if (h > frameRows || w > frameCols)
            throw new IllegalArgumentException("shape " + h + "x" + w + " is larger than the " + frameRows + "x" + frameCols + " frame");
        final var g = new int[frameRows * frameCols];
        for (var r = 0; r < h; r++)
            for (var c = 0; c < lines.get(r).length(); c++) {
                final var ch = lines.get(r).charAt(c);
                if (ch == 'x' || ch == 'X' || ch == '1') g[r * frameCols + c] = 1;
            }
        return g;
    }

    private Main() { }
}
