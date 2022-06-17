package org.int4.dirk.spi.scope;

import java.lang.annotation.Annotation;

import org.int4.dirk.api.scope.ScopeNotActiveException;

/**
 * Handles resolving of types with a specific scope annotation.
 */
public interface ScopeResolver {

  /**
   * Returns the annotation this resolver handles. Returns {@code null} when
   * the handler handles unscoped injectables.
   *
   * @return the annotation this resolver handles, never {@code null}
   */
  Annotation getAnnotation();

  /**
   * Returns {@code true} when this scope is currently active, otherwise {@code false}.
   *
   * @return {@code true} when this scope is currently active, otherwise {@code false}
   */
  boolean isActive();

  /**
   * Returns an existing instance associated with the given key, or uses the
   * given context to create one.
   *
   * @param <T> the type of the instances provided by the {@link CreationalContext}
   * @param key an object suitable as a key for use in a map, cannot be {@code null}
   * @param creationalContext a {@link CreationalContext}, cannot be {@code null}
   * @return an instance of the given type, never {@code null}
   * @throws ScopeNotActiveException when there is no scope active
   * @throws Exception when the object factory throws an exception
   */
  <T> T get(Object key, CreationalContext<T> creationalContext) throws ScopeNotActiveException, Exception;

  /**
   * Find an existing {@link CreationalContext} by key in the current active scope,
   * or return {@code null} if no scope is active or no context was found with the given
   * key.
   *
   * @param key an object suitable as a key for use in a map, cannot be {@code null}
   * @return a {@link CreationalContext} if found, otherwise {@code null}
   */
  CreationalContext<?> find(Object key);

  /**
   * Removes the given key from this scope resolver.
   *
   * @param key an object suitable as a key for use in a map, cannot be {@code null}
   */
  void remove(Object key);

}
