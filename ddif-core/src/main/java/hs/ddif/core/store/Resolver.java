package hs.ddif.core.store;

import java.util.Set;

/**
 * Implementers provide a way to look up {@link QualifiedType}s by {@link Key}.
 *
 * @param <T> a {@link QualifiedType} type
 */
public interface Resolver<T extends QualifiedType> {

  /**
   * Look up {@link QualifiedType}s by {@link Key}. The empty set is returned if
   * there were no matches.
   *
   * @param key the {@link Key}, cannot be {@code null}
   * @return a set of {@link QualifiedType}s matching the given {@link Key}, never {@code null} but can be empty
   */
  Set<T> resolve(Key key);
}
