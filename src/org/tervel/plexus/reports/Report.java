package org.tervel.plexus.reports;

import org.tervel.plexus.Operation;

/**
 * A report is an {@link Operation} whose output is a human-readable {@code String}: a unary converter
 * from a trained {@link org.tervel.plexus.Plexus} to text. All concrete reports implement this.
 */
public interface Report extends Operation<String> { }
