package hs.ddif.core.store;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Implementers provide a way to look up {@link Injectable}s by {@link Type} and criteria.
 *
 * @param <T> an {@link Injectable} type
 */
public interface Resolver<T extends Injectable> {

  /**
   * Look up {@link Injectable}s by {@link Type} with the given criteria.  The empty set is returned if
   * there were no matches.  Supported criteria are:
   * <ul>
   * <li>{@link Class} to match by implemented interface or by presence of an annotation, for
   *     example the interface <code>List.class</code> or the annotation
   *     <code>Singleton.class</code></li>
   * <li>{@link Annotation} to match by an annotation, including matching all of its values</li>
   * <li>{@link hs.ddif.core.api.Matcher} to match by custom criteria provided by a {@code Matcher}
   *     implementation</li>
   * </ul>
   *
   * @param type the {@link Type} of the {@link Injectable}s to look up, cannot be null
   * @param criteria zero or more additional criteria the {@link Injectable}s must match
   * @return a set of {@link Injectable}s matching the given type and criteria, never null but can be empty
   */
  Set<T> resolve(Type type, Object... criteria);
}
