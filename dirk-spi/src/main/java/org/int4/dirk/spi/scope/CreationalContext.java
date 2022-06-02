package org.int4.dirk.spi.scope;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;

/**
 * Context used to create and destroy instances with injection information.
 *
 * @param <T> the type of instance this context can produce
 */
public interface CreationalContext<T> {

  /**
   * Gets an instance of type {@code T}. A new instance is created during the first call,
   * and the same instance is returned for subsequent calls.
   *
   * <p>Throws {@link IllegalStateException} when the context was released.
   *
   * @return an instance of type {@code T}, can be {@code null}
   * @throws CreationException when an instance could not be created
   * @throws AmbiguousResolutionException when multiple instances could be created but at most one was required
   * @throws UnsatisfiedResolutionException when no instance could be created but at least one was required
   * @throws IllegalStateException when the context was released
   */
  T get() throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException;

  /**
   * Releases this context. Any dependent objects associated with it
   * will be destroyed. This method is idempotent.
   */
  void release();
}
