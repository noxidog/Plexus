package org.tervel.plexus;

/**
 * What a shape's bounding region turned out to be, which fixes its root, its symmetry walk, and its trailing
 * coordinates.
 *
 * <ul>
 *   <li>{@link #EMPTY}: no pixels; a single key under the D4 root.</li>
 *   <li>{@link #RECT}: a rectangular box on the square lattice; enveloped to landscape, walked over D2.</li>
 *   <li>{@link #SQUARE}: a square box that stayed on the square lattice; walked over D4.</li>
 *   <li>{@link #HEX}: a square box that won the hex comparison; sheared, walked over D6, filed under the D6 root.</li>
 * </ul>
 */
public enum Kind { EMPTY, RECT, SQUARE, HEX }
