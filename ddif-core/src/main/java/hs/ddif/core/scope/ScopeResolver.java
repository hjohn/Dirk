package hs.ddif.core.scope;

import hs.ddif.core.instantiation.injection.Constructable;

import java.lang.annotation.Annotation;

/**
 * Handles resolving of types with a specific scope annotation.
 */
public interface ScopeResolver {

  /**
   * Returns the annotation this resolver handles. Returns {@code null} when
   * the handler handles unscoped injectables.
   *
   * @return the annotation this resolver handles, can be {@code null}
   */
  Class<? extends Annotation> getAnnotationClass();

  /**
   * Returns {@code true} when a scope is currently active, otherwise {@code false}.
   *
   * @return {@code true} when a scope is currently active, otherwise {@code false}
   */
  boolean isActive();

  /**
   * Returns an instance of the given type or constructing it if needed.
   *
   * @param <T> the type of the instances provided by the {@link Constructable}
   * @param constructable a {@link Constructable} (suitable as a key for use in a map), cannot be {@code null}
   * @param injectionContext an {@link InjectionContext}, cannot be {@code null}
   * @return an instance of the given type, never {@code null}
   * @throws OutOfScopeException when there is no scope active
   * @throws Exception when the object factory throws an exception
   */
  <T> T get(Constructable<T> constructable, InjectionContext injectionContext) throws OutOfScopeException, Exception;

  /**
   * Removes the {@link Constructable} from this scope resolver.
   *
   * @param <T> the type of the instances provided by the {@link Constructable}
   * @param constructable a {@link Constructable} (suitable as a key for use in a map), cannot be {@code null}
   */
  <T> void remove(Constructable<T> constructable);

  /**
   * Checks if this scope resolver represents a singleton scope.
   *
   * @return {@code true} if this scope resolver represents a singleton scope, otherwise {@code false}
   */
  default boolean isSingleton() {
    return false;
  }
}
