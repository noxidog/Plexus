package org.tervel.plexus;

/**
 * What a read concludes.
 *
 * <ul>
 *   <li>{@link #POSITIVE} / {@link #NEGATIVE}: the grid's whole path was trained; the leaf (with its exact
 *       exceptions) answers.</li>
 *   <li>{@link #ASSUMED}: the path breaks off at a pure node the extrapolation setting lets stand in for the missing child; the
 *       answer is that node's label, never seen for this exact value.</li>
 *   <li>{@link #UNSEEN}: the path breaks off where no stand-in is allowed (a mixed node, or a pure node above
 *       what the setting allows). Not a negative: an absence of evidence.</li>
 * </ul>
 */
public enum Verdict { POSITIVE, NEGATIVE, UNSEEN, ASSUMED }
