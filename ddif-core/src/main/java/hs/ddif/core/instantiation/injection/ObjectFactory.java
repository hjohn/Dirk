package hs.ddif.core.instantiation.injection;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;

/**
 * Provides an object, potentially constructing it directly or indirectly and
 * any other objects that it depends on.
 *
 * @param <T> the type of the instances produced
 */
public interface ObjectFactory<T> {

  /**
   * Creates an instance.
   *
   * @param injectionContext an {@link InjectionContext}, cannot be {@code null}
   * @return an instance, or {@code null} if it could not be provided
   * @throws InstanceCreationFailure when instantiation fails
   */
  T createInstance(InjectionContext injectionContext) throws InstanceCreationFailure;

  /**
   * Destroys an instance.
   *
   * @param instance an instance to destroy, cannot be {@code null}
   * @param injectionContext an {@link InjectionContext} used to create the instance, cannot be {@code null}
   */
  void destroyInstance(T instance, InjectionContext injectionContext);
}
