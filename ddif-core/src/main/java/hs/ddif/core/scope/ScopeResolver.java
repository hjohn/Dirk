package hs.ddif.core.scope;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;

import javax.inject.Scope;

/**
 * Handles resolving of types with a specific {@link Scope} annotation.
 */
public interface ScopeResolver {

  /**
   * Returns the annotation this resolver handles.
   *
   * @return the annotation this resolver handles, never {@code null}
   */
  Class<? extends Annotation> getScopeAnnotationClass();

  /**
   * Returns {@code true} when a scope is currently active, otherwise {@code false}.
   *
   * @return {@code true} when a scope is currently active, otherwise {@code false}
   */
  boolean isScopeActive();

  /**
   * Returns an instance of the given type or constructs it using the given object factory.
   *
   * @param <T> the type of the instance
   * @param key an {@link Object} (suitable as a key for use in a map), cannot be {@code null}
   * @param objectFactory a {@link Callable} which serves as an object factory, cannot be {@code null}
   * @return an instance of the given type, never {@code null}
   * @throws OutOfScopeException when there is no scope active
   * @throws Exception when the object factory throws an exception
   */
  <T> T get(Object key, Callable<T> objectFactory) throws OutOfScopeException, Exception;

  /**
   * Removes the given object (or key) from this scope resolver.
   *
   * @param key an {@link Object} (suitable as a key for use in a map), cannot be {@code null}
   */
  void remove(Object key);
}
