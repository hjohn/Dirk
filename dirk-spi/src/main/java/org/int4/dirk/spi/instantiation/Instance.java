package org.int4.dirk.spi.instantiation;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;

/**
 * Allows dynamic access to instances created by an injector. An {@link Instance} can
 * be used to create instances of type {@code T} which match all its criteria. Sub
 * {@link Instance}s can be created which further narrow the set of possible matches.
 *
 * <p>{@link Instance}s are created for specific injection targets. This target may be
 * marked optional. If the target is optional, {@code null} is a valid
 * result in case the {@link Instance} can provide no matches.
 *
 * @param <T> the type of the instances
 */
public interface Instance<T> {

  /**
   * Creates an instance of type {@code T}. If there are multiple matching instances
   * an {@link AmbiguousResolutionException} exception is thrown. If the target for
   * which this {@link Instance} was created is optional, then {@code null} is returned when
   * there are no matches, otherwise an {@link UnsatisfiedResolutionException} is thrown.
   *
   * @return an instance or {@code null} if there were no matches
   * @throws CreationException when the creation of the instance failed
   * @throws UnsatisfiedResolutionException when nothing matched and {@code null} is not valid
   * @throws AmbiguousResolutionException when their are multiple potential instances
   * @throws ScopeNotActiveException when the scope for the given type is not active
   */
  T get() throws CreationException, UnsatisfiedResolutionException, AmbiguousResolutionException, ScopeNotActiveException;

  /**
   * Creates all instances of type {@code T}. If the target for which this {@link Instance} was
   * created is optional, then {@code null} is returned when there are no matches, otherwise an
   * empty list is returned.
   *
   * <p>Scoped types which scope is currently inactive are ignored, and so this call
   * never produces scope related exceptions.
   *
   * @return a list of instances, can be {@code null} or empty but never contains {@code null}
   * @throws CreationException when the creation of an instance failed
   */
  List<T> getAll() throws CreationException;

  /**
   * Destroys an instance created with this {@link Instance} or one of its children.
   *
   * @param instance an instance to destroy, cannot be {@code null}
   */
  void destroy(T instance);

  /**
   * Destroys a collection of instances created with this {@link Instance} or one of its children.
   *
   * @param instances a collection of instances to destroy, cannot be {@code null}
   */
  void destroyAll(Collection<T> instances);

  /**
   * Creates an {@link Instance} based on this one which further
   * narrows the potential matching instances by requiring additional qualifiers.
   *
   * @param qualifiers an array of additional qualifier {@link Annotation}s, cannot be {@code null}
   * @return an {@link Instance}, never {@code null}
   * @throws IllegalArgumentException if any of the given annotations is not a qualifier
   */
  Instance<T> select(Annotation... qualifiers);

  /**
   * Creates an {@link Instance} based on this one which further
   * narrows the potential matching instances to a subtype of {@code T} and the given
   * additional qualifiers.
   *
   * @param <U> a subtype of type {@code T}
   * @param subtype a subtype of type {@code T}, cannot be {@code null}
   * @param qualifiers an array of additional qualifier {@link Annotation}s, cannot be {@code null}
   * @return an {@link Instance}, never {@code null}
   * @throws IllegalArgumentException if any of the given annotations is not a qualifier
   */
  <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers);

  /**
   * Creates an {@link Instance} based on this one which further
   * narrows the potential matching instances to a subtype of {@code T} and the given
   * additional qualifiers.
   *
   * @param <U> a subtype of type {@code T}
   * @param subtype specifies a subtype of type {@code T}, cannot be {@code null}
   * @param qualifiers an array of additional qualifier {@link Annotation}s, cannot be {@code null}
   * @return an {@link Instance}, never {@code null}
   * @throws IllegalArgumentException if any of the given annotations is not a qualifier
   */
  <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers);

}
