package hs.ddif.core.instantiation.factory;

import hs.ddif.core.definition.LifeCycleCallbacksFactory;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;

/**
 * Interface for calling life cycle methods on a given instance.
 */
public interface LifeCycleCallbacks {

  /**
   * Calls all post construct life cycle methods which were discovered by the
   * {@link LifeCycleCallbacksFactory} on an instance of the same type.
   *
   * @param instance an instance, cannot be {@code null}
   * @throws InstanceCreationFailure when a callback failed
   */
  void postConstruct(Object instance) throws InstanceCreationFailure;

  /**
   * Calls all pre-destroy life cycle methods which were discovered by the
   * {@link LifeCycleCallbacksFactory} on an instance of the same type.
   *
   * @param instance an instance, cannot be {@code null}
   */
  void preDestroy(Object instance);
}