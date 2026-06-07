package org.tervel.plexus.invariants;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * The shape signature: a {@link Mass} ({@code V}) and a {@link Connectivity} ({@code E}) coordinate
 * <b>composed into one injective column</b> — a composite invariant. They are bundled on purpose —
 * <em>separately each is blind in one direction:</em>
 *
 * <ul>
 *   <li>Mass alone admits any negative of the same cell-count but different structure (a connected shape
 *       vs. a shattered blob of equal mass slip through together).</li>
 *   <li>Connectivity alone admits any negative of the same structure but different size.</li>
 * </ul>
 *
 * Only their <b>conjunction</b> pins a shape's size <em>and</em> its structure, so neither is a correct
 * invariant by itself; together they are exactly one.
 *
 * <h2>Why this is legal: the invariance contract</h2>
 * A routing coordinate may only be a quantity that is <b>invariant under the routing symmetry</b>, or it
 * would send orbit-mates to different contexts and shatter the class. Both components qualify: every rigid
 * transform in D4 is a bijection on cells that maps orthogonal adjacencies to orthogonal adjacencies, so
 * it preserves {@code V} and {@code E} exactly. (Contrast {@code Position}, which is <em>not</em> invariant
 * and so cannot be packed in here.)
 *
 * <h2>The packing</h2>
 * {@code value = V·W + E} with {@code W = maxEdges + 1}, so distinct {@code (V, E)} pairs map to distinct
 * values — "same signature" iff "same mass <b>and</b> same connectivity". The components are recoverable
 * via {@link #massOf} / {@link #edgesOf}, which {@code Fiber} uses to read the mass back off the key.
 */
public final class Signature implements Invariant {

    private final Invariant mass;
    private final Invariant connectivity;
    private final int base;

    /**
     * Pack a {@code mass} and a {@code connectivity} coordinate into one injective column for a
     * {@code side×side} grid. The base is {@link #width} = {@code maxEdges + 1}, so it strictly exceeds any
     * connectivity value and the {@code (V, E)} packing stays injective.
     */
    public Signature(Invariant mass, Invariant connectivity, int side) {
        this.mass = mass;
        this.connectivity = connectivity;
        this.base = width(side);
    }

    /** Packing base: one more than the maximum possible edge count on a {@code side×side} grid. */
    public static int width(int side) { return 2 * side * (side - 1) + 1; }

    /** The mass {@code V} encoded in a signature value. */
    public static int massOf(int value, int side) { return value / width(side); }

    /** The connectivity {@code E} encoded in a signature value. */
    public static int edgesOf(int value, int side) { return value % width(side); }

    @Override
    public int applyAsInt(int[] g) {
        return mass.applyAsInt(g) * base + connectivity.applyAsInt(g);
    }

    @Override public boolean applies(boolean radial) { return mass.applies(radial) && connectivity.applies(radial); }
    @Override public String name() { return "sig"; }

    /**
     * Unpack into the two structural scalars the packing hides — {@code V} ({@link Mass}) and {@code E}
     * ({@link Connectivity}). The packed value is for keying; the determinant/Jacobian volume differentiates
     * these two clean, D4-invariant rows instead (see {@link Invariant#components()}).
     */
    @Override public List<ToIntFunction<int[]>> components() { return List.of(mass, connectivity); }
}
