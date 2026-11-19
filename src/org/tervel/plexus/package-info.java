/**
 * Plexus: a symmetry-routed exact classifier. A router, not a network: no weights, no thresholds, no statistics.
 *
 * <p>Every input grid is filed by what its shape is, not where it sits. {@link org.tervel.plexus.Descriptor} reads
 * the bounding box of the non-zero pixels, picks the lattice (square, or hex when a square box gains symmetry by
 * shearing), turns the shape into a canonical pose, and answers the symmetry walk. {@link org.tervel.plexus.Plexus}
 * keeps two tries, one per lattice, keyed
 *
 * <pre>
 * D4 root / D6 root → box → symmetry walk → mass → joins → [extensions] → orientation → pose → corner
 * </pre>
 *
 * <p>Reads return {@link org.tervel.plexus.Verdict POSITIVE, NEGATIVE, ASSUMED or UNSEEN} under a
 * {@link org.tervel.plexus.Extrapolate}; {@link org.tervel.plexus.Slice slices} walk the same tries under constraints and
 * stream every matching grid, trained or assumed, to a callback. {@code cli.Main} is the interpreter. See the
 * {@code docs/} directory for the design.
 */
package org.tervel.plexus;
