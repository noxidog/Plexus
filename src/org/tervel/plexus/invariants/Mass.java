package org.tervel.plexus.invariants;

/**
 * Mass: the active-cell count {@code V}. D4-invariant — every rigid transform is a bijection on cells,
 * so the count survives — hence a legal routing coordinate. Blind alone (any equal-count shape, however
 * differently structured, collides), so it is packed with {@link Connectivity} into a {@link Signature}.
 */
public final class Mass implements Invariant {

    @Override
    public int applyAsInt(int[] g) {
        var mass = 0;
        for (final var cell : g) if (cell == 1) mass++;
        return mass;
    }

    @Override public boolean applies(boolean radial) { return true; }
    @Override public String name() { return "mass"; }
}
