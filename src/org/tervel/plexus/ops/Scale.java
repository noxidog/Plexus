package org.tervel.plexus.ops;

import org.tervel.plexus.Operation;
import org.tervel.plexus.Plexus;
import org.tervel.plexus.symmetry.Dihedral;
import org.tervel.plexus.symmetry.Shape;
import org.tervel.plexus.symmetry.SymmetryGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Coarse-grain a grid by <b>block-decimation</b> — the change-of-scale (renormalization) direction, run as
 * an {@link Operation} and built as the explicit sibling of {@link Decompose}. Where {@code Decompose} folds
 * <em>one shape's cells</em> into atoms (the orbit partition under that shape's stabilizer, §9), {@code Scale}
 * folds <em>a whole grid's cells</em> into a coarser grid: each {@code k×k} block collapses to one coarse
 * cell. Both are the §9 fold read backward; the only difference is the unit of folding (a strand vs. a block)
 * and what the fold discards — {@code Decompose} discards orbit-copies (recorded as {@code isotropy});
 * {@code Scale} discards a block's microstates (recorded here as its D4-<b>orbit</b> size, the same quantity).
 *
 * <h2>The block fold is the classical decimation (§11)</h2>
 * The {@link Rule} is to {@code Scale} what {@code CutLaw} is to {@code Decompose}: the one knob. It keeps
 * <em>one coarse bit</em> per block and forgets which of the block's microstates produced it — that is the
 * §11 fold-to-representative (stationary phase, the classical limit), applied per block. The entropy the
 * fold throws away is {@code log|orbit|} per block; {@link Coarsening#integratedEntropy()} sums it — the
 * degrees of freedom integrated out, the "loop content" the coarse bit can no longer see. The unbuilt
 * <b>quantum</b> move would keep those microstates with a phase and sum amplitudes instead of collapsing;
 * we only <em>measure</em> the discarded count here, we do not sum it.
 *
 * <h2>The regulator constraint (D4-equivariance)</h2>
 * Decimation must commute with the symmetry group, or it breaks the invariance contract the way a
 * position-sensitive invariant would (§3) — the analogue of a regulator that preserves the symmetry. With
 * {@code k | side} the block tiling is centre-symmetric, and an intrinsic D4-invariant per-block readout
 * (occupancy / mass) makes {@code Scale(t·g) == t'·Scale(g)} for the induced coarse transform {@code t'}.
 *
 * <p><b>Size note.</b> Clean decimation needs {@code k | side} with {@code k ≥ 2}; a meaningful fold wants
 * {@code side ≥ 4}. On the 3×3 reference set only {@code k = 3} divides the side (a degenerate 1×1 coarse
 * grid) — fine as a smoke test, but the renormalization <em>flow</em> needs a larger grid.
 */
public final class Scale implements Operation<Scale.Coarsening> {

    /** The block fold = the classical decimation rule: how a k×k block's mass collapses to one coarse bit. */
    public enum Rule {
        /** Coarse bit = 1 iff any fine cell is set — the canonical fold's occupancy. The principled default. */
        CANONICAL,
        /** Coarse bit = 1 iff any fine cell is set. (Same bit as CANONICAL; kept for naming symmetry.) */
        OCCUPANCY,
        /** Coarse bit = 1 iff a strict majority of the block is set (mass is D4-invariant, so ties are too). */
        MAJORITY
    }

    /**
     * One decimated block: where it sits in the fine grid, the fine cells it covered, the canonical
     * {@link Shape} the fold collapsed it to, that shape's D4-{@code orbit} size (the microstates the fold
     * discarded — the sibling of {@code Decompose}'s {@code isotropy}), and the surviving coarse bit.
     */
    public record Block(int top, int left, List<Integer> fineCells, Shape canonical, long orbit, int coarseBit) { }

    /**
     * The result: the coarse grid (and its side), the per-block fold record, and the knobs. The headline
     * derived quantity is {@link #integratedEntropy()} — the total {@code log|orbit|} the decimation threw
     * away, the term renormalization must track and the place the unbuilt quantum phase-sum would live.
     */
    public record Coarsening(int[] coarse, int coarseSide, List<Block> blocks, Rule rule, int k) {

        /** The degrees of freedom integrated out by the fold: {@code Σ log2(orbit)} over the blocks (bits). */
        public double integratedEntropy() {
            return blocks.stream().mapToDouble(b -> Math.log(b.orbit()) / Math.log(2)).sum();
        }

        /** A human rendering: the rule, the coarse grid, each block's fold, and the integrated entropy. */
        public String render() {
            final var sb = new StringBuilder();
            sb.append("scale [").append(rule).append("]  k=").append(k)
              .append("  (each ").append(k).append('x').append(k)
              .append(" block folds to one coarse cell; |orbit| = microstates discarded)\n");
            sb.append("  coarse grid (").append(coarseSide).append('x').append(coarseSide).append("):\n");
            for (var r = 0; r < coarseSide; r++) {
                sb.append("    ");
                for (var c = 0; c < coarseSide; c++) sb.append(coarse[r * coarseSide + c] == 1 ? 'x' : '.');
                sb.append('\n');
            }
            sb.append("  blocks (").append(blocks.size()).append("):\n");
            for (final var b : blocks)
                sb.append(String.format("    (%d,%d)  bit=%d  %-10s |orbit|=%d  isotropy %s%n",
                        b.top(), b.left(), b.coarseBit(), ascii(b.canonical()), b.orbit(),
                        b.canonical().isotropyFraction()));
            sb.append(String.format("  integrated entropy (DOF folded away): %.3f bits  "
                    + "(the loop content the coarse bit cannot see)%n", integratedEntropy()));
            return sb.toString();
        }
    }

    private final int[] grid;
    private final int k;
    private final Rule rule;

    public Scale(int[] grid, int k, Rule rule) {
        this.grid = grid.clone();
        this.k = k;
        this.rule = rule;
    }

    @Override
    public Coarsening apply(Plexus plexus) {
        final var side = (int) Math.round(Math.sqrt(grid.length));
        if (k < 1 || side % k != 0)
            throw new IllegalArgumentException("k=" + k + " must divide side=" + side + " (clean decimation)");
        final var coarseSide = side / k;
        final SymmetryGroup blockGroup = Dihedral.d4();   // side-agnostic: transforms read side=k from the block

        final var coarse = new int[coarseSide * coarseSide];
        final var blocks = new ArrayList<Block>();
        for (var br = 0; br < coarseSide; br++)
            for (var bc = 0; bc < coarseSide; bc++) {
                final var top = br * k;
                final var left = bc * k;
                final var block = new int[k * k];
                final var fineCells = new ArrayList<Integer>();
                var mass = 0;
                for (var r = 0; r < k; r++)
                    for (var c = 0; c < k; c++) {
                        final var fine = (top + r) * side + (left + c);
                        if (grid[fine] == 1) { block[r * k + c] = 1; mass++; fineCells.add(fine); }
                    }
                final var bit = coarseBit(mass);
                coarse[br * coarseSide + bc] = bit;
                final var shape = Shape.of(block, blockGroup);
                blocks.add(new Block(top, left, List.copyOf(fineCells), shape, orbitSize(shape), bit));
            }
        return new Coarsening(coarse, coarseSide, List.copyOf(blocks), rule, k);
    }

    /** The coarse bit for a block of the given mass, under the chosen rule. */
    private int coarseBit(int mass) {
        return switch (rule) {
            case CANONICAL, OCCUPANCY -> mass > 0 ? 1 : 0;
            case MAJORITY -> mass * 2 > k * k ? 1 : 0;
        };
    }

    /** The block's D4-orbit size = {@code 1 / isotropy} — the microstates the canonical fold discarded. */
    private static long orbitSize(Shape s) {
        return s.isotropy() <= 0 ? 1 : Math.round(1.0 / s.isotropy());
    }

    /** Compact {@code "row/row"} rendering of a canonical shape's pattern (e.g. {@code "xx/x."}). */
    private static String ascii(Shape s) {
        if (s.height() == 0) return ".";
        final var sb = new StringBuilder();
        for (var r = 0; r < s.height(); r++) {
            for (var c = 0; c < s.width(); c++) sb.append(s.cells()[r * s.width() + c] == 1 ? 'x' : '.');
            if (r < s.height() - 1) sb.append('/');
        }
        return sb.toString();
    }
}
