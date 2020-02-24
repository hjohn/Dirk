package hs.ddif.core.scope;

import java.lang.annotation.Annotation;

import javax.inject.Scope;

/**
 * Handles resolving of beans with a specific {@link Scope} annotation.
 */
public interface ScopeResolver {

  /**
   * Returns the annotation this resolver handles.
   *
   * @return the annotation this resolver handles, never null
   */
  Class<? extends Annotation> getScopeAnnotationClass();

  /**
   * Returns an instance of the given class or <code>null</code> if no instance is
   * associated with the current scope.
   *
   * @param injectableClass a {@link Class}
   * @return an instance of the given class or <code>null</code> if no instance is associated with the current scope
   * @throws OutOfScopeException when there is no scope active
   */
  <T> T get(Class<?> injectableClass);

  /**
   * Stores the given instance for a given class in the current active scope.
   *
   * @param injectableClass a {@link Class}
   * @param instance an instance to associate with the current scope and given class
   * @throws OutOfScopeException when there is no scope active
   */
  <T> void put(Class<?> injectableClass, T instance);
}
