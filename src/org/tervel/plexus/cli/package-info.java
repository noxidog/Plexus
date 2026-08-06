/**
 * The command-line application: parse a dataset, train a {@link org.tervel.plexus.Plexus}, and drive the
 * interactive REPL (§8).
 *
 * <p>It sits in its own package because it is an <b>application</b>, not part of the library. Everything
 * under {@code org.tervel.plexus} except this package is consumed as an API; {@code Main} is consumed by a
 * user at a terminal, and it owns the concerns that go with that — argument handling, {@code Scanner} input,
 * {@code System.out} formatting, and the dataset file format. Keeping it separate means the dependency runs
 * one way only: the CLI knows about the router, the router knows nothing about the CLI.
 *
 * <p>The same rule keeps experiment runners out of the core. Anything with a {@code main} is either an
 * application (here) or an assertion that belongs as a test (in {@code test/}); the library itself has no
 * entry points.
 */
package org.tervel.plexus.cli;
