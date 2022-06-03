package org.int4.dirk.api;

import java.lang.reflect.Type;
import java.util.List;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;

/**
 * Provides methods to resolve types and classes to injected instances.
 *
 * <p>All methods support filtering by qualifier annotation, by providing either an {@link java.lang.annotation.Annotation}
 * instance (obtainable via {@code Annotations#of(Class)}) or by providing an annotation
 * {@link Class} directly (for marker annotations only). Annotations should be qualifier annotations
 * or no matches will be found.
 *
 * <p>Filtering by generic type is possible by creating a corresponding {@link TypeLiteral}.
 *
 * <p>Examples:<br>
 *
 * <pre>getInstance(Database.class)</pre>
 *
 * would return a {@code Database} instance.
 *
 * <pre>getInstance(<b>new TypeLiteral&lt;Provider&lt;Database>>() {}</b>)</pre>
 *
 * would return a {@code Provider} for a {@code Database} instance.
 *
 * <pre>getInstances(Vehicle.class, Red.class)</pre>
 *
 * or
 *
 * <pre>getInstances(Vehicle.class, Annotations.of(Red.class))</pre>
 *
 * would return all {@code Vehicle}s instances annotated with the {@code @Red} qualifier annotation.
 *
 * <pre>getInstance(String.class, Annotations.of(Named.class, Map.of("value", "config.url"))</pre>
 *
 * would return a {@code String} instance which was registered with a {@code Named} annotation with
 * value "config.url".
 */
public interface InstanceResolver {

  /**
   * Returns an instance of the type specified by the given {@link TypeLiteral} matching the given
   * criteria (if any) in which all dependencies are injected. The instance returned can either
   * be an existing instance or newly created depending on its scope.
   *
   * @param <T> the type of the instance
   * @param typeLiteral specifies the type of the instance required, cannot be {@code null}
   * @param qualifiers optional list of qualifier annotations, either {@link java.lang.annotation.Annotation} or {@link Class}&lt;? extends Annotation&gt;
   * @return an instance of the given class matching the given criteria, never {@code null}
   * @throws UnsatisfiedResolutionException when no matching instance was available or could be created
   * @throws AmbiguousResolutionException when multiple matching instances were available
   * @throws CreationException when an error occurred during creation of a matching instance
   * @throws ScopeNotActiveException when the scope for the produced type is not active
   */
  <T> T getInstance(TypeLiteral<T> typeLiteral, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException;

  /**
   * Returns an instance of the given class matching the given criteria (if any) in
   * which all dependencies are injected. The instance returned can either
   * be an existing instance or newly created depending on its scope.
   *
   * @param <T> the type of the instance
   * @param cls the class of the instance required, cannot be {@code null}
   * @param qualifiers optional list of qualifier annotations, either {@link java.lang.annotation.Annotation} or {@link Class}&lt;? extends Annotation&gt;
   * @return an instance of the given class matching the given criteria (if any)
   * @throws UnsatisfiedResolutionException when no matching instance was available or could be created
   * @throws AmbiguousResolutionException when multiple matching instances were available
   * @throws CreationException when an error occurred during creation of a matching instance
   * @throws ScopeNotActiveException when the scope for the produced type is not active
   */
  <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, ScopeNotActiveException;

  /**
   * Returns all instances of the type specified by the given {@link TypeLiteral} matching the given
   * criteria (if any) in which all dependencies are injected.  When there are no matches, an empty
   * set is returned. The instances returned can either be existing instances or newly created
   * depending on their scope or a mix thereof.
   *
   * @param <T> the type of the instances
   * @param typeLiteral specifies the type of the instances required, cannot be {@code null}
   * @param qualifiers optional list of qualifier annotations, either {@link java.lang.annotation.Annotation} or {@link Class}&lt;? extends Annotation&gt;
   * @return all instances of the given {@link Type} matching the given criteria (if any), never {@code null}, can be empty
   * @throws CreationException when an error occurred during creation of a matching instance
   */
  <T> List<T> getInstances(TypeLiteral<T> typeLiteral, Object... qualifiers) throws CreationException;

  /**
   * Returns all instances of the given class matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned. The instances returned can either be existing instances or newly created
   * depending on their scope or a mix thereof.
   *
   * @param <T> the type of the instances
   * @param cls the class of the instances required, cannot be {@code null}
   * @param qualifiers optional list of qualifier annotations, either {@link java.lang.annotation.Annotation} or {@link Class}&lt;? extends Annotation&gt;
   * @return all instances of the given class matching the given criteria (if any), never {@code null}, can be empty
   * @throws CreationException when an error occurred during creation of a matching instance
   */
  <T> List<T> getInstances(Class<T> cls, Object... qualifiers) throws CreationException;
}
