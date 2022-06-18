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
 * A context which can be used to create instances of type {@code T} which match
 * all its criteria. Sub contexts can be created which further narrow the set of
 * possible matches.
 *
 * <p>Contexts are often created for a specific injection target. This target may
 * allow optional injection. If optional injection is allowed, {@code null} is a valid
 * result in case there are no matches.
 *
 * @param <T> the type of the instances
 */
public interface InstantiationContext<T> {

  /**
   * Creates an instance of type {@code T}. If there are multiple matching instances key given
   * an {@link AmbiguousResolutionException} exception is thrown. Returns {@code null} if
   * there are no matches and {@code null} is a considered valid result for this context.
   *
   * @return an instance or {@code null} if there were no matches
   * @throws CreationException when the creation of the instance failed
   * @throws UnsatisfiedResolutionException when nothing matched and {@code null} is not valid
   * @throws AmbiguousResolutionException when their are multiple potential instances
   * @throws ScopeNotActiveException when the scope for the given type is not active
   */
  T create() throws CreationException, UnsatisfiedResolutionException, AmbiguousResolutionException, ScopeNotActiveException;

  /**
   * Creates all instances of type {@code T} this context can provide. If there were no
   * matches {@code null} is returned if valid for this context, otherwise returns an empty
   * list. Scoped types which scope is currently inactive are excluded.
   *
   * @return a list of instances, can be {@code null} or empty but never contains {@code null}
   * @throws CreationException when the creation of an instance failed
   */
  List<T> createAll() throws CreationException;

  /**
   * Destroys an instance created with this {@link InstantiationContext} or one of its children.
   *
   * @param instance an instance to destroy, cannot be {@code null}
   */
  void destroy(T instance);

  /**
   * Destroys a collection of instances created with this {@link InstantiationContext} or one of its children.
   *
   * @param instances a collection of instances to destroy, cannot be {@code null}
   */
  void destroyAll(Collection<T> instances);

  /**
   * Creates an {@link InstantiationContext} based on this context which further
   * narrows the potential matching instances by requiring additional qualifiers.
   *
   * @param qualifiers an array of additional qualifier {@link Annotation}s, cannot be {@code null}
   * @return an {@link InstantiationContext}, never {@code null}
   * @throws IllegalArgumentException if any of the given annotations is not a qualifier
   */
  InstantiationContext<T> select(Annotation... qualifiers);

  /**
   * Creates an {@link InstantiationContext} based on this context which further
   * narrows the potential matching instances to a subtype of {@code T} and the given
   * additional qualifiers.
   *
   * @param <U> a subtype of type {@code T}
   * @param subtype a subtype of type {@code T}, cannot be {@code null}
   * @param qualifiers an array of additional qualifier {@link Annotation}s, cannot be {@code null}
   * @return an {@link InstantiationContext}, never {@code null}
   * @throws IllegalArgumentException if any of the given annotations is not a qualifier
   */
  <U extends T> InstantiationContext<U> select(Class<U> subtype, Annotation... qualifiers);

  /**
   * Creates an {@link InstantiationContext} based on this context which further
   * narrows the potential matching instances to a subtype of {@code T} and the given
   * additional qualifiers.
   *
   * @param <U> a subtype of type {@code T}
   * @param subtype specifies a subtype of type {@code T}, cannot be {@code null}
   * @param qualifiers an array of additional qualifier {@link Annotation}s, cannot be {@code null}
   * @return an {@link InstantiationContext}, never {@code null}
   * @throws IllegalArgumentException if any of the given annotations is not a qualifier
   */
  <U extends T> InstantiationContext<U> select(TypeLiteral<U> subtype, Annotation... qualifiers);

}
