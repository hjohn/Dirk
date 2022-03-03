package hs.ddif.core.instantiation.injection;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;

import java.util.List;

/**
 * Provides an object, potentially constructing it directly or indirectly and
 * any other objects that it depends on.
 */
public interface ObjectFactory {

  /**
   * Creates an instance.
   *
   * @param injections a list of {@link Injection} containing values to be injected, never {@code null} or contains {@code null}s but can be empty
   * @return an instance, or {@code null} if it could not be provided
   * @throws InstanceCreationFailure when instantiation fails
   */
  Object createInstance(List<Injection> injections) throws InstanceCreationFailure;

  /**
   * Destroys an instance.
   *
   * @param instance an instance to destroy, cannot be {@code null}
   * @param injections a list of {@link Injection} used to create the instance, cannot be {@code null} or contain {@code null}s but can be empty
   */
  void destroyInstance(Object instance, List<Injection> injections);
}
