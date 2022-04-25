package hs.ddif.api;

import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;

/**
 * Provides methods to resolve {@link Type}s to instances.
 *
 * <p>All methods support filtering by qualifier annotation, by providing either an {@link java.lang.annotation.Annotation}
 * instance (obtainable via {@code Annotations#of(Class)}) or by providing a
 * {@link Class} instance of &lt;? extends Annotation&gt;. Annotations should be qualifier annotations or
 * no matches will be found.
 *
 * <p>Methods that can return multiple instances also support a {@link Predicate} of {@link Type} to allow
 * custom filtering.
 *
 * <p>Filtering by generic type is possible by providing {@link java.lang.reflect.ParameterizedType} or a {@link java.lang.reflect.WildcardType}.
 * There are various ways to construct such types, see for example {@code hs.ddif.util.Types} and
 * {@code hs.ddif.util.TypeReference}.
 *
 * <p>Examples:<br>
 *
 * <pre>getInstance(Database.class)</pre>
 *
 * would return a {@code Database} instance.
 *
 * <pre>getInstance(Types.wildcardExtends(Database.class, Queryable.class))</pre>
 *
 * would return an object which implements or extends both {@code Database} and {@code Queryable}.
 *
 * <pre>getInstances(Vehicle.class, Red.class)</pre>
 *
 * or
 *
 * <pre>getInstances(Vehicle.class, Annotations.of(Red.class))</pre>
 *
 * would return all {@code Vehicle}s instances annotated with the {@code @Red} qualifier annotation.
 *
 * <pre>getInstance(String.class, Annotations.named("config.url")</pre>
 *
 * or
 *
 * <pre>getInstance(String.class, Annotations.of(Named.class, Map.of("value", "config.url"))</pre>
 *
 * would return a {@code String} instance which was registered with a {@code Named} annotation with
 * value "config.url".
 */
public interface InstanceResolver {

  /**
   * Returns an instance of the given {@link Type} matching the given criteria (if any) in
   * which all dependencies are injected. The instance returned can either
   * be an existing instance or newly created depending on its scope.
   *
   * @param <T> the type of the instance
   * @param type the type of the instance required, cannot be {@code null}
   * @param qualifiers optional list of qualifier annotations, either {@link java.lang.annotation.Annotation} or {@link Class}&lt;? extends Annotation&gt;
   * @return an instance of the given class matching the given criteria, never {@code null}
   * @throws UnsatisfiedResolutionException when no matching instance was available or could be created
   * @throws AmbiguousResolutionException when multiple matching instances were available
   * @throws CreationException when an error occurred during creation of a matching instance
   * @throws AutoDiscoveryException when the injector is configured for auto discovery, the type requested was not registered yet and discovery of the type and its dependencies failed
   */
  <T> T getInstance(Type type, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, AutoDiscoveryException;

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
   * @throws AutoDiscoveryException when the injector is configured for auto discovery, the type requested was not registered yet and discovery of the type and its dependencies failed
   */
  <T> T getInstance(Class<T> cls, Object... qualifiers) throws UnsatisfiedResolutionException, AmbiguousResolutionException, CreationException, AutoDiscoveryException;  // The signature of this method closely matches the other getInstance method as Class implements Type, however, this method will auto-cast the result thanks to the type parameter

  /**
   * Returns all instances of the given {@link Type} matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned. The instances returned can either be existing instances or newly created
   * depending on their scope or a mix thereof.
   *
   * @param <T> the type of the instances
   * @param type the {@link Type} of the instances required, cannot be {@code null}
   * @param qualifiers optional list of qualifier annotations, either {@link java.lang.annotation.Annotation} or {@link Class}&lt;? extends Annotation&gt;
   * @return all instances of the given {@link Type} matching the given criteria (if any), never {@code null}, can be empty
   * @throws CreationException when an error occurred during creation of a matching instance
   */
  <T> List<T> getInstances(Type type, Object... qualifiers) throws CreationException;

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
