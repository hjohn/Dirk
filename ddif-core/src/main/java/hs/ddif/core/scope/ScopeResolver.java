package hs.ddif.core.scope;

import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;

import javax.inject.Scope;

/**
 * Handles resolving of types with a specific {@link Scope} annotation.
 */
public interface ScopeResolver {

  /**
   * Returns the annotation this resolver handles.
   *
   * @return the annotation this resolver handles, never null
   */
  Class<? extends Annotation> getScopeAnnotationClass();

  /**
   * Returns {@code true} when a scope is currently active, otherwise {@code false}.
   *
   * @param key an {@link Injectable} (suitable as a key for use in a map), cannot be null
   * @return {@code true} when a scope is currently active, otherwise {@code false}
   */
  boolean isScopeActive(Injectable key);

  /**
   * Returns an instance of the given type or <code>null</code> if no instance is
   * associated with the current scope.
   *
   * @param <T> the type of the instance
   * @param key an {@link Injectable} (suitable as a key for use in a map), cannot be null
   * @return an instance of the given class or <code>null</code> if no instance is associated with the current scope
   * @throws OutOfScopeException when there is no scope active
   */
  <T> T get(Injectable key) throws OutOfScopeException;

  /**
   * Stores the given instance for a given type in the current active scope.
   *
   * @param <T> the type of the instance
   * @param key an {@link Injectable} (suitable as a key for use in a map), cannot be null
   * @param instance an instance to associate with the current scope and given class, cannot be null
   * @throws OutOfScopeException when there is no scope active
   */
  <T> void put(Injectable key, T instance) throws OutOfScopeException;
}
