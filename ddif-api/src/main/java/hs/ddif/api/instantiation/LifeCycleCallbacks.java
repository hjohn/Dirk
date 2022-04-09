package hs.ddif.api.instantiation;

import hs.ddif.api.instantiation.domain.InstanceCreationFailure;

/**
 * Interface for calling life cycle methods on a given instance.
 */
public interface LifeCycleCallbacks {

  /**
   * Calls all post construct life cycle methods which were discovered on a
   * type on an instance of the same type.
   *
   * @param instance an instance, cannot be {@code null}
   * @throws InstanceCreationFailure when a callback failed
   */
  void postConstruct(Object instance) throws InstanceCreationFailure;

  /**
   * Calls all pre-destroy life cycle methods which were discovered on a
   * type on an instance of the same type.
   *
   * @param instance an instance, cannot be {@code null}
   */
  void preDestroy(Object instance);
}