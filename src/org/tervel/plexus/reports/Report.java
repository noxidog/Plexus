package org.tervel.plexus.reports;

import org.tervel.plexus.Operation;

/**
 * A report is an {@link Operation} whose output is a human-readable {@code String}: a unary converter
 * from a trained {@link org.tervel.plexus.Plexus} to text. All concrete reports implement this.
 */
public interface Report extends Operation<String> {

    /**
     * Render one grid as an ASCII block on its <b>actual {@code rows×cols} frame</b> (the row stride is
     * {@code cols}), not an assumed square. Reports that key off real input grids read the frame from the
     * {@link org.tervel.plexus.Plexus} ({@code plexus.rows(g)} / {@code plexus.cols(g)}) so a rectangular
     * domain renders with its true aspect rather than a {@code sqrt(length)} square.
     */
    static void renderGrid(StringBuilder sb, String indent, int[] g, int rows, int cols, char on, char off) {
        for (var r = 0; r < rows; r++) {
            sb.append(indent);
            for (var c = 0; c < cols; c++) sb.append(g[r * cols + c] == 1 ? on : off);
            sb.append('\n');
        }
    }
}
