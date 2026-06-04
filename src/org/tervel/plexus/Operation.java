package org.tervel.plexus;

import java.util.function.Function;

/**
 * The basic abstraction over a trained {@link Plexus}: a unary function that consumes a Plexus and
 * produces some result of type {@code T}. Reports are {@code Operation<String>}; a search is an
 * {@code Operation<Iterator<…>>}; future operations pick whatever output type they need. The Plexus
 * is the fixed input; everything an operation needs to configure itself it carries in its own state.
 */
@FunctionalInterface
public interface Operation<T> extends Function<Plexus, T> { }
