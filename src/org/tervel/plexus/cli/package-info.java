/**
 * The command-line application: parse a dataset, train a {@link org.tervel.plexus.Plexus}, and drive the
 * interactive REPL.
 *
 * <p>It sits in its own package because it is an <b>application</b>, not part of the library. Everything
 * under {@code org.tervel.plexus} except this package is consumed as an API; {@code Main} is consumed by a
 * user at a terminal, and it owns the concerns that go with that: argument handling, {@code Scanner} input,
 * {@code System.out} formatting, and the dataset file format. The dependency runs one way only: the CLI knows
 * about the router, the router knows nothing about the CLI.
 */
package org.tervel.plexus.cli;
