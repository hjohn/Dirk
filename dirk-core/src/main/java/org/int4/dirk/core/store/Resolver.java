package org.int4.dirk.core.store;

import java.util.Set;

import org.int4.dirk.core.definition.Key;

/**
 * Implementers provide a way to look up a type {@code T} by {@link Key}.
 *
 * @param <T> the resolved type
 */
public interface Resolver<T> {

  /**
   * Look up types {@code T} by {@link Key}. The empty set is returned if
   * there were no matches.
   *
   * @param key the {@link Key}, cannot be {@code null}
   * @return a set of type {@code T}s matching the given {@link Key}, never {@code null} but can be empty
   */
  Set<T> resolve(Key key);
}