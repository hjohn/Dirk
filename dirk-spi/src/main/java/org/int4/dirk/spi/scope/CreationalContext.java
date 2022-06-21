package org.int4.dirk.spi.scope;

/**
 * Context used to create and destroy instances with injection information.
 *
 * @param <T> the type of instance this context can produce
 */
public interface CreationalContext<T> {

  /**
   * Gets an instance of type {@code T}.
   *
   * <p>Throws {@link IllegalStateException} when the context was released.
   *
   * @return an instance of type {@code T}, can be {@code null}
   * @throws IllegalStateException when the context was released
   */
  T get();

  /**
   * Releases this context. Any dependent objects associated with it
   * will be destroyed. This method is idempotent.
   */
  void release();

}
