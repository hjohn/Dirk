package hs.ddif.core.store;

import java.util.Set;

/**
 * Implementers provide a way to look up {@link Injectable}s by {@link Key}.
 *
 * @param <T> an {@link Injectable} type
 */
public interface Resolver<T extends Injectable> {

  /**
   * Look up {@link Injectable}s by {@link Key}. The empty set is returned if
   * there were no matches.
   *
   * @param key the {@link Key}, cannot be null
   * @return a set of {@link Injectable}s matching the given {@link Key}, never null but can be empty
   */
  Set<T> resolve(Key key);
}
