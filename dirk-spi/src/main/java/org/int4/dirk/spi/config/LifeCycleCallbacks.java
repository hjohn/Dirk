package org.int4.dirk.spi.config;

import java.lang.reflect.InvocationTargetException;

/**
 * Interface for calling life cycle methods on a given instance.
 */
public interface LifeCycleCallbacks {

  /**
   * Calls all post construct life cycle methods which were discovered on a
   * type on an instance of the same type.
   *
   * @param instance an instance, cannot be {@code null}
   * @throws InvocationTargetException when a callback threw an exception
   */
  void postConstruct(Object instance) throws InvocationTargetException;

  /**
   * Calls all pre-destroy life cycle methods which were discovered on a
   * type on an instance of the same type.
   *
   * @param instance an instance, cannot be {@code null}
   */
  void preDestroy(Object instance);

  /**
   * Checks whether any calls have been registered for the pre-destroy life cycle phase.
   *
   * @return {@code true} if the pre-destroy life cycle phase must be executed, otherwise {@code false}
   */
  boolean needsDestroy();
}