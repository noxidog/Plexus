package org.tervel.plexus;

/**
 * The result of a read.
 *
 * @param verdict what was concluded
 * @param label   the answer: 1 or 0 for POSITIVE, NEGATIVE and ASSUMED; −1 for UNSEEN
 * @param context the node that controls the answer (for UNSEEN, the node where the path broke off)
 */
public record Answer(Verdict verdict, int label, Context context) { }
