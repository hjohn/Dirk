package hs.ddif.core.store;

import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public interface Resolver<T extends Injectable> {

  /**
   * Looks up Injectables by type and by the given criteria.  The empty set is returned if
   * there were no matches.  Supported criteria are:
   * <ul>
   * <li>{@link Class} to match by implemented interface or by presence of an annotation, for
   *     example the interface <code>List.class</code> or the annotation
   *     <code>Singleton.class</code></li>
   * <li>{@link Annotation} or {@link AnnotationDescriptor} to match by an annotation,
   *     including matching all its values</li>
   * <li>{@link Matcher} to match by custom criteria provided by a {@link Matcher}
   *     implementation</li>
   * </ul>
   * @param type the type of the Injectables to look up
   * @param criteria the criteria the Injectables must match
   * @return a set of Injectables matching the given type and critera
   */
  Set<T> resolve(Type type, Object... criteria);
}
