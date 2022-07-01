package org.int4.dirk.spi.instantiation;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;

/**
 * Provides instances of type {@code T} created from elements of type {@code E}.
 *
 * @param <T> the type handled
 * @param <E> the element type required
 */
public interface InstanceProvider<T, E> {

  /**
   * Creates an instance of type {@code T} using the given {@link Instance}.
   * Returning {@code null} is allowed to indicate the absence of a value. Depending on the
   * destination where the value is used this can mean {@code null} is injected (methods and
   * constructors), that the value is not injected (fields) or that an {@link UnsatisfiedResolutionException}
   * is thrown (direct instance resolver call).
   *
   * @param instance an {@link Instance}, cannot be {@code null}
   * @return an instance of type {@code T}, can be {@code null}
   * @throws CreationException when the instance could not be created
   * @throws AmbiguousResolutionException when multiple instances matched but at most one was required
   * @throws UnsatisfiedResolutionException when no instance matched but at least one was required
   * @throws ScopeNotActiveException when the scope for the produced type is not active
   */
  T getInstance(Instance<E> instance);
}