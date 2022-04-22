package hs.ddif.spi.scope;

import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;

/**
 * Context used to create and destroy instances with injection information.
 *
 * @param <T> the type of instance this context can produce
 */
public interface CreationalContext<T> {

  /**
   * Creates a new instance of type {@code T} wrapped in a {@link Reference}.
   *
   * @return a {@link Reference}, never {@code null}
   * @throws CreationException when an instance could not be created
   * @throws AmbiguousResolutionException when multiple instances could be created but at most one was required
   * @throws UnsatisfiedResolutionException when no instance could be created but at least one was required
   */
  Reference<T> create() throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException;

  /**
   * A reference to an instance created by the {@link CreationalContext} with
   * which the created instance can be released.
   *
   * @param <T> the type of instance this reference holds
   */
  interface Reference<T> {

    /**
     * Gets the instance held by this reference. If the reference has already
     * been released this will throw {@link IllegalStateException}.
     *
     * @return the instance held by this reference, can be {@code null}
     * @throws IllegalStateException when the reference was released
     */
    T get();

    /**
     * Releases this reference. Any dependent objects associated with it
     * will be destroyed. This method is idempotent.
     */
    void release();
  }
}
