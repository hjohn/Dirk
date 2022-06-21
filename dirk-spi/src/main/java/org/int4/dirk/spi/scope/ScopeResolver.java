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
   * Find an existing {@link CreationalContext} by key in the current active scope,
   * or return {@code null} if no context was found in the given scope with the given
   * key.
   *
   * @param key an object suitable as a key for use in a map, cannot be {@code null}
   * @return a {@link CreationalContext} if found, otherwise {@code null}
   * @throws ScopeNotActiveException when there is no scope active
   */
  CreationalContext<?> find(Object key) throws ScopeNotActiveException;

  /**
   * Adds a {@link CreationalContext} to this scope resolver under the given key.
   *
   * @param key an object suitable as a key for use in a map, cannot be {@code null}
   * @param creationalContext a {@link CreationalContext}, cannot be {@code null}
   * @throws ScopeNotActiveException when there is no scope active
   */
  void put(Object key, CreationalContext<?> creationalContext) throws ScopeNotActiveException;

  /**
   * Removes the given key from this scope resolver.
   *
   * @param key an object suitable as a key for use in a map, cannot be {@code null}
   */
  void remove(Object key);

}
