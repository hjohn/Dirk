package hs.ddif.core.api;

import hs.ddif.core.util.Annotations;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Provides methods to resolve queries, consisting of a {@link Type} and free form
 * criteria, to instances.<p>
 *
 * Supported criteria that can be used in queries are:
 * <ul>
 * <li>A {@link Class} to match by implemented interface or {@link javax.inject.Qualifier}
 *     annotation, for example the interface <code>Comparable</code> or the annotation
 *     <code>Named</code></li>
 * <li>An {@link java.lang.annotation.Annotation} to match by annotation, including matching
 *     all of its values; {@code Annotation} instances can be obtained using {@link Annotations}
 *     helper class</li>
 * <li>A {@link java.util.function.Predicate} to match by custom criteria provided by a {@link java.util.function.Predicate}
 *     implementation</li>
 * </ul>
 *
 * Examples:<br>
 *
 * <pre>getInstance(Database.class, Queryable.class)</pre>
 *
 * would return a {@code Database} instance which implements the {@code Queryable} interface.
 *
 * <pre>getInstances(Vehicle.class, Red.class)</pre>
 *
 * would return all {@code Vehicle}s instances annotated with the {@code @Red} qualifier annotation.
 *
 * <pre>getInstance(String.class, AnnotationDescriptor.named("config.url")</pre>
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
   * @param criterions optional list of criteria, see {@link InstanceResolver}
   * @return an instance of the given class matching the given criteria, never {@code null}
   * @throws NoSuchInstanceException when no matching instance was available or could be created
   * @throws MultipleInstancesException when multiple matching instances were available
   * @throws InstanceCreationException when an error occurred during creation of a matching instance
   */
  <T> T getInstance(Type type, Object... criterions);

  /**
   * Returns an instance of the given class matching the given criteria (if any) in
   * which all dependencies are injected. The instance returned can either
   * be an existing instance or newly created depending on its scope.
   *
   * @param <T> the type of the instance
   * @param cls the class of the instance required, cannot be {@code null}
   * @param criterions optional list of criteria, see {@link InstanceResolver}
   * @return an instance of the given class matching the given criteria (if any)
   * @throws NoSuchInstanceException when no matching instance was available or could be created
   * @throws MultipleInstancesException when multiple matching instances were available
   * @throws InstanceCreationException when an error occurred during creation of a matching instance
   */
  <T> T getInstance(Class<T> cls, Object... criterions);  // The signature of this method closely matches the other getInstance method as Class implements Type, however, this method will auto-cast the result thanks to the type parameter

  /**
   * Returns all instances of the given {@link Type} matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned. The instances returned can either be existing instances or newly created
   * depending on their scope or a mix thereof.
   *
   * @param <T> the type of the instances
   * @param type the {@link Type} of the instances required, cannot be {@code null}
   * @param criterions optional list of criteria, see {@link InstanceResolver}
   * @return all instances of the given {@link Type} matching the given criteria (if any), never {@code null}, can be empty
   * @throws InstanceCreationException when an error occurred during creation of a matching instance
   */
  <T> List<T> getInstances(Type type, Object... criterions);

  /**
   * Returns all instances of the given class matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned. The instances returned can either be existing instances or newly created
   * depending on their scope or a mix thereof.
   *
   * @param <T> the type of the instances
   * @param cls the class of the instances required, cannot be {@code null}
   * @param criteria optional list of criteria, see {@link InstanceResolver}
   * @return all instances of the given class matching the given criteria (if any), never {@code null}, can be empty
   * @throws InstanceCreationException when an error occurred during creation of a matching instance
   */
  <T> List<T> getInstances(Class<T> cls, Object... criteria);
}
