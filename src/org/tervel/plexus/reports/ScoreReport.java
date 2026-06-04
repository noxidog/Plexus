package org.tervel.plexus.reports;

import org.tervel.plexus.Plexus;

import java.util.List;

/**
 * Evaluates a trained Plexus against a labeled dataset and reports how well its
 * predictions match: a confusion matrix, accuracy, and the base-rate-robust MCC.
 */
public class ScoreReport implements Report {

    private static final int SAMPLE_FALSE_POSITIVES = 8;

    private final List<int[]> inputs;
    private final List<Integer> labels;

    public ScoreReport(List<int[]> inputs, List<Integer> labels) {
        this.inputs = inputs;
        this.labels = labels;
    }

    @Override
    public String apply(Plexus plexus) {
        long tp = 0, fp = 0, fn = 0, tn = 0;
        final var recalled = new java.util.ArrayList<int[]>();
        final var missed = new java.util.ArrayList<int[]>();
        final var falsePositives = new java.util.ArrayList<int[]>();
        for (var i = 0; i < inputs.size(); i++) {
            final var predicted = plexus.score(inputs.get(i));
            final var actual = labels.get(i);
            if (predicted == 1 && actual == 1) {
                tp++;
                recalled.add(inputs.get(i));
            } else if (predicted == 1) {
                fp++;
                falsePositives.add(inputs.get(i));
            } else if (actual == 1) {
                fn++;
                missed.add(inputs.get(i));
            } else {
                tn++;
            }
        }

        final var total = tp + fp + fn + tn;
        final var correct = tp + tn;
        final var accuracy = total == 0 ? 0.0 : (double) correct / total;
        final var denom = Math.sqrt((double) (tp + fp) * (tp + fn) * (tn + fp) * (tn + fn));
        final var mcc = denom == 0.0 ? 0.0 : (tp * (double) tn - fp * (double) fn) / denom;

        final var sb = new StringBuilder();
        sb.append("Plexus evaluation over ").append(total).append(" examples\n");
        sb.append(String.format("  accuracy = %.3f (%d/%d)%n", accuracy, correct, total));
        sb.append(String.format("  mcc      = %+.3f%n", mcc));
        sb.append(String.format("  actual T = %d   predicted T = %d%n", tp + fn, tp + fp));
        sb.append(String.format("  TP=%d  FP=%d  FN=%d  TN=%d%n", tp, fp, fn, tn));
        appendGrids(sb, "recalled (true-positive T's)", recalled);
        appendGrids(sb, String.format("false negatives (%d, missed T's)", fn), missed);
        // Show the most-confident false positives — the ones in the most positive-leaning contexts,
        // i.e. the patterns the model is most convinced are T's despite being negatives.
        falsePositives.sort(java.util.Comparator.<int[]>comparingDouble(g -> confidence(plexus, g)).reversed());
        final var sample = Math.min(SAMPLE_FALSE_POSITIVES, falsePositives.size());
        appendGrids(sb, String.format("false positives (top %d of %d by score)", sample, falsePositives.size()),
                falsePositives.subList(0, sample));
        return sb.toString();
    }

    /** A grid's context confidence: the positive fraction of the bucket it routes to (0 if unseen). */
    private static double confidence(Plexus plexus, int[] g) {
        final var c = plexus.context(g);
        if (c == null) return 0.0;
        final var total = c.pos() + c.neg();
        return total == 0 ? 0.0 : (double) c.pos() / total;
    }

    /** Renders each grid as a side×side block of 'x'/'_' under a heading; prints "(none)" if empty. */
    private static void appendGrids(StringBuilder sb, String heading, List<int[]> grids) {
        if (grids.isEmpty()) {
            sb.append("  ").append(heading).append(": (none)\n");
            return;
        }
        sb.append("  ").append(heading).append(":\n");
        final var side = (int) Math.sqrt(grids.get(0).length);
        for (final var grid : grids) {
            for (var r = 0; r < side; r++) {
                sb.append("    ");
                for (var c = 0; c < side; c++) {
                    sb.append(grid[r * side + c] == 1 ? 'x' : '_');
                }
                sb.append('\n');
            }
            sb.append('\n');
        }
    }
}
